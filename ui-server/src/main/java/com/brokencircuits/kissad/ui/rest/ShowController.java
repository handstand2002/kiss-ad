package com.brokencircuits.kissad.ui.rest;

import com.brokencircuits.kissad.Translator;
import com.brokencircuits.kissad.kafka.AdminInterface;
import com.brokencircuits.kissad.kafka.ByteKey;
import com.brokencircuits.kissad.kafka.KeyValueStoreWrapper;
import com.brokencircuits.kissad.kafka.Publisher;
import com.brokencircuits.kissad.messages.ShowMsg;
import com.brokencircuits.kissad.messages.ShowMsgKey;
import com.brokencircuits.kissad.messages.SourceName;
import com.brokencircuits.kissad.topics.TopicUtil;
import com.brokencircuits.kissad.ui.rest.domain.HsShowObject;
import com.brokencircuits.kissad.ui.rest.domain.ShowObject;
import com.brokencircuits.kissad.util.Uuid;
import com.brokencircuits.messages.Command;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.scheduling.support.SimpleTriggerContext;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ShowController {

  private final Publisher<ByteKey<ShowMsgKey>, ShowMsg> showMessagePublisher;
  private final Translator<ShowObject, KeyValue<ByteKey<ShowMsgKey>, ShowMsg>> showLocalToMsgTranslator;
  private final Translator<KeyValue<ByteKey<ShowMsgKey>, ShowMsg>, ShowObject> showMsgToLocalTranslator;
  private final Translator<HsShowObject, KeyValue<ByteKey<ShowMsgKey>, ShowMsg>> hsShowLocalToMsgTranslator;

  private final KeyValueStoreWrapper<ByteKey<ShowMsgKey>, ShowMsg> showMsgStore;
  private final AdminInterface adminInterface;

  private static final SimpleDateFormat NEXT_EPISODE_DATE_FORMAT = new SimpleDateFormat(
      "EEE h:mma");

  @Value("${show.default.episode-name-pattern}")
  private String defaultEpisodeNamePattern;
  @Value("${show.default.release-schedule-cron}")
  private String defaultReleaseScheduleCron;

  @RequestMapping("/show/{id}")
  public String show(@PathVariable Uuid id, Model model) {
    ByteKey<ShowMsgKey> lookupKey = new ByteKey<>(ShowMsgKey.newBuilder().setShowId(id).build());
    ShowMsg showMessage = showMsgStore.get(lookupKey);

    model.addAttribute("sourceTypes", SourceName.values());

    if (showMessage != null) {
      model.addAttribute("show",
          showMsgToLocalTranslator.translate(KeyValue.pair(lookupKey, showMessage)));
    }
    return "show";
  }

  @RequestMapping(value = "/shows", method = RequestMethod.GET)
  public String showsList(Model model) {
    List<ShowObject> outputList = new ArrayList<>();
    try (KeyValueIterator<ByteKey<ShowMsgKey>, ShowMsg> iterator = showMsgStore.all()) {
      iterator.forEachRemaining(pair -> outputList.add(showMsgToLocalTranslator.translate(pair)));
    }
    log.info("Found {} shows", outputList.size());
    outputList.forEach(show -> log.info("Show: {}", show));

    List<ShowObject> sortedShows = outputList.stream().sorted(getShowScheduleComparator())
        .map(showObject -> {
          Date nextRun = nextRunTime(showObject.getReleaseScheduleCron());
          if (nextRun != null) {
            showObject.setNextEpisode(NEXT_EPISODE_DATE_FORMAT.format(nextRun));
          }
          return showObject;
        })
        .collect(Collectors.toList());

    model.addAttribute("shows", sortedShows);
    return "shows";
  }

  private static Date nextRunTime(String cron) {
    Date date = null;
    try {
      CronTrigger trigger1 = new CronTrigger(cron);
      date = trigger1.nextExecutionTime(new SimpleTriggerContext());
    } catch (IllegalArgumentException ignored) {

    }
    return date;
  }

  private static Comparator<ShowObject> getShowScheduleComparator() {
    return (o1, o2) -> {
      Date nextTrigger1 = nextRunTime(o1.getReleaseScheduleCron());
      Date nextTrigger2 = nextRunTime(o2.getReleaseScheduleCron());

      if (nextTrigger1 == null && nextTrigger2 == null) {
        return 0;
      } else if (nextTrigger1 == null) {
        return -1;
      } else if (nextTrigger2 == null) {
        return 1;
      } else {
        return nextTrigger1.compareTo(nextTrigger2);
      }
    };
  }

  @RequestMapping(value = "/addShow", method = RequestMethod.GET)
  public String addShow(Model model) {

    model.addAttribute("sourceTypes", SourceName.values());
    return "addShow";
  }

  @RequestMapping(value = "/addShow", method = RequestMethod.POST)
  public String addShow(ShowObject showObject, Model model) {

    if (showObject.getIsActive() == null) {
      showObject.setIsActive(true);
    }

    if (showObject.getShowId() == null) {
      showObject.setShowId(Uuid.randomUUID());
    }

    if (showObject.getEpisodeNamePattern() == null) {
      showObject.setEpisodeNamePattern(defaultEpisodeNamePattern);
    }

    if (showObject.getReleaseScheduleCron() == null) {
      showObject.setReleaseScheduleCron(defaultReleaseScheduleCron);
    }

    KeyValue<ByteKey<ShowMsgKey>, ShowMsg> msg = showLocalToMsgTranslator.translate(showObject);

    if (msg.value.getValue().getSkipEpisodeString() != null && !msg.value.getValue()
        .getSkipEpisodeString().isEmpty()) {
      adminInterface.sendCommand(TopicUtil.MODULE_DOWNLOAD_DELEGATOR, Command.SKIP_EPISODE_RANGE,
          msg.value.getKey().getShowId().toString(), msg.value.getValue().getSkipEpisodeString());

      msg.value.getValue().setSkipEpisodeString(null);
    }

    showMessagePublisher.send(msg);
    return "redirect:/shows";
  }

  @RequestMapping(path = "/deleteShow/{id}")
  public String deleteShow(@PathVariable final Uuid id, Model model) {
    ByteKey<ShowMsgKey> lookupKey = new ByteKey<>(ShowMsgKey.newBuilder().setShowId(id).build());
    ShowMsg showMessage = showMsgStore.get(lookupKey);
    ShowObject showObject = null;
    if (showMessage != null && showMessage.getValue() != null) {
      showObject = showMsgToLocalTranslator
          .translate(new KeyValue<>(lookupKey, showMessage));
    }
    if (showObject != null) {
      showMessagePublisher.send(lookupKey, null);
    }
    return "redirect:/shows";
  }

  @RequestMapping(path = "/checkShow")
  public String checkShow() {
    showMsgStore.all()
        .forEachRemaining(pair -> checkShow(pair.value.getKey().getShowId()));

    return "redirect:/shows";
  }

  @RequestMapping(path = "/checkShow/{id}")
  public String checkShow(@PathVariable final Uuid id) {
    ByteKey<ShowMsgKey> lookupKey = ByteKey.from(ShowMsgKey.newBuilder().setShowId(id).build());
    ShowMsg showMessage = showMsgStore.get(lookupKey);

    if (showMessage != null) {
      adminInterface
          .sendCommand(TopicUtil.MODULE_SCHEDULER, Command.CHECK_NEW_EPISODES, id.toString());
    }

    return "redirect:/shows";
  }

}
