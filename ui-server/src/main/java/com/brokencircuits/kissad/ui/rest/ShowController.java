package com.brokencircuits.kissad.ui.rest;

import com.brokencircuits.kissad.Translator;
import com.brokencircuits.kissad.kafka.AdminInterface;
import com.brokencircuits.kissad.kafka.ByteKey;
import com.brokencircuits.kissad.kafka.KeyValueStoreWrapper;
import com.brokencircuits.kissad.kafka.Publisher;
import com.brokencircuits.kissad.messages.EpisodeMsg;
import com.brokencircuits.kissad.messages.EpisodeMsgKey;
import com.brokencircuits.kissad.messages.ShowMsg;
import com.brokencircuits.kissad.messages.ShowMsgKey;
import com.brokencircuits.kissad.messages.SourceName;
import com.brokencircuits.kissad.topics.TopicUtil;
import com.brokencircuits.kissad.ui.rest.domain.EpisodeObject;
import com.brokencircuits.kissad.ui.rest.domain.ShowObject;
import com.brokencircuits.kissad.ui.rest.domain.ShowObject.ShowObjectBuilder;
import com.brokencircuits.kissad.util.Uuid;
import com.brokencircuits.messages.Command;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
  private final Publisher<ByteKey<EpisodeMsgKey>, EpisodeMsg> showEpisodePublisher;
  private final Translator<ShowObject, KeyValue<ByteKey<ShowMsgKey>, ShowMsg>> showLocalToMsgTranslator;
  private final Translator<KeyValue<ByteKey<ShowMsgKey>, ShowMsg>, ShowObject> showMsgToLocalTranslator;

  private final KeyValueStoreWrapper<ByteKey<ShowMsgKey>, ShowMsg> showMsgStore;
  private final KeyValueStoreWrapper<ByteKey<EpisodeMsgKey>, EpisodeMsg> episodeStore;
  private final AdminInterface adminInterface;

  private static final SimpleDateFormat NEXT_EPISODE_DATE_FORMAT = new SimpleDateFormat(
      "EEE h:mma");
  private static final DateTimeFormatter downloadTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd h:mm a");

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

  @RequestMapping("/showEpisodes/{id}")
  public String showEpisodes(@PathVariable Uuid id, Model model) {
    ByteKey<ShowMsgKey> lookupKey = new ByteKey<>(ShowMsgKey.newBuilder().setShowId(id).build());
    ShowMsg showMessage = showMsgStore.get(lookupKey);

    model.addAttribute("sourceTypes", SourceName.values());

    if (showMessage != null) {
      model.addAttribute("show",
          showMsgToLocalTranslator.translate(KeyValue.pair(lookupKey, showMessage)));
    }

    Map<Long, EpisodeObject> downloadedEpisodes = new HashMap<>();

    try (KeyValueIterator<ByteKey<EpisodeMsgKey>, EpisodeMsg> iterator = episodeStore.all()) {
      iterator.forEachRemaining(kv -> {
        if (kv.value.getKey().getShowId().getShowId().equals(id)) {
          downloadedEpisodes.put(kv.value.getKey().getEpisodeNumber(), EpisodeObject.builder()
              .episodeNumber(kv.value.getKey().getEpisodeNumber())
              .downloadedQuality(kv.value.getValue().getDownloadedQuality())
              .downloadTime(kv.value.getValue().getDownloadTime()
                  .atZone(ZoneId.systemDefault())
                  .toLocalDateTime()
                  .format(downloadTimeFormatter))
              .build());
        }
      });
    }
    model.addAttribute("episodes", downloadedEpisodes.values().stream()
        .sorted(Comparator.comparingLong(EpisodeObject::getEpisodeNumber).reversed())
        .collect(Collectors.toList()));
    return "showEpisodes";
  }

  @RequestMapping(value = "/shows", method = RequestMethod.GET)
  public void showsList(Model model) {
    List<ShowObject> outputList = new ArrayList<>();
    try (KeyValueIterator<ByteKey<ShowMsgKey>, ShowMsg> iterator = showMsgStore.all()) {
      iterator.forEachRemaining(pair -> outputList.add(showMsgToLocalTranslator.translate(pair)));
    }
    log.info("Found {} shows", outputList.size());
    outputList.forEach(show -> log.info("Show: {}", show));

    List<ShowObject> sortedShows = outputList.stream().sorted(getShowScheduleComparator())
        .map(showObject -> {
          ShowObjectBuilder builder = showObject.toBuilder();
          Date nextRun = nextRunTime(showObject.getReleaseScheduleCron());
          if (nextRun != null) {
            builder.nextEpisode(NEXT_EPISODE_DATE_FORMAT.format(nextRun));
          }
          return builder.build();
        })
        .collect(Collectors.toList());

    model.addAttribute("shows", sortedShows);
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

  @RequestMapping(path = "/deleteEpisode/{showId}/{epNum}")
  public String deleteShow(@PathVariable final Uuid showId, @PathVariable final Long epNum) {
    EpisodeMsgKey epKey = EpisodeMsgKey.newBuilder()
        .setShowId(ShowMsgKey.newBuilder()
            .setShowId(showId)
            .build())
        .setEpisodeNumber(epNum)
        .build();
    ByteKey<EpisodeMsgKey> key = new ByteKey<>(epKey);
    showEpisodePublisher.send(key, null);

    return "redirect:/showEpisodes/" + showId;
  }

}
