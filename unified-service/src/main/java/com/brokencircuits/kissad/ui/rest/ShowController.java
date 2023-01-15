package com.brokencircuits.kissad.ui.rest;

import com.brokencircuits.kissad.Translator;
import com.brokencircuits.kissad.kafka.ByteKey;
import com.brokencircuits.kissad.kafka.KeyValue;
import com.brokencircuits.kissad.kafka.table.KafkaBackedTable;
import com.brokencircuits.kissad.messages.EpisodeMsg;
import com.brokencircuits.kissad.messages.EpisodeMsgKey;
import com.brokencircuits.kissad.messages.EpisodeMsgValue;
import com.brokencircuits.kissad.messages.ShowMsg;
import com.brokencircuits.kissad.messages.ShowMsgKey;
import com.brokencircuits.kissad.messages.SourceName;
import com.brokencircuits.kissad.ui.rest.domain.EpisodeObject;
import com.brokencircuits.kissad.ui.rest.domain.ShowObject;
import com.brokencircuits.kissad.ui.rest.domain.ShowObject.ShowObjectBuilder;
import com.brokencircuits.kissad.util.Uuid;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
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

  private final KafkaBackedTable<ByteKey<EpisodeMsgKey>, EpisodeMsg> episodeTable;
  private final KafkaBackedTable<ByteKey<ShowMsgKey>, ShowMsg> showTable;
  private final Translator<ShowObject, KeyValue<ByteKey<ShowMsgKey>, ShowMsg>> showLocalToMsgTranslator;
  private final Translator<KeyValue<ByteKey<ShowMsgKey>, ShowMsg>, ShowObject> showMsgToLocalTranslator;
  private final Consumer<Uuid> triggerShowCheckMethod;
  private final TaskExecutor taskExecutor;

  private static final SimpleDateFormat NEXT_EPISODE_DATE_FORMAT = new SimpleDateFormat(
      "EEE h:mma");
  private static final DateTimeFormatter downloadTimeFormatter = DateTimeFormatter.ofPattern(
      "yyyy-MM-dd h:mm a");

  @Value("${show.default.episode-name-pattern}")
  private String defaultEpisodeNamePattern;
  @Value("${show.default.release-schedule-cron}")
  private String defaultReleaseScheduleCron;

  @RequestMapping("/show/{id}")
  public String show(@PathVariable Uuid id, Model model) {
    ByteKey<ShowMsgKey> lookupKey = new ByteKey<>(ShowMsgKey.newBuilder().setShowId(id).build());
    ShowMsg showMessage = showTable.get(lookupKey);
    if (showMessage == null) {
      AtomicLong counter = new AtomicLong(0);
      showTable.all(kv -> counter.incrementAndGet());
      log.info("showTable has {} entries", counter.get());
      log.info("Lookup Key: {}", Arrays.toString(lookupKey.getBytes()));
      throw new IllegalStateException("Could not find show");
    }

    model.addAttribute("sourceTypes", SourceName.values());

    model.addAttribute("show",
        showMsgToLocalTranslator.translate(KeyValue.of(lookupKey, showMessage)));
    return "show";
  }

  @RequestMapping("/showEpisodes/{id}")
  public String showEpisodes(@PathVariable Uuid id, Model model) {
    ByteKey<ShowMsgKey> lookupKey = new ByteKey<>(ShowMsgKey.newBuilder().setShowId(id).build());
    ShowMsg showMessage = showTable.get(lookupKey);

    model.addAttribute("sourceTypes", SourceName.values());

    if (showMessage != null) {
      model.addAttribute("show",
          showMsgToLocalTranslator.translate(KeyValue.of(lookupKey, showMessage)));
    }

    Map<Long, EpisodeObject> downloadedEpisodes = new HashMap<>();

    episodeTable.all(kv -> {
      if (kv.getValue().getKey().getShowId().getShowId().equals(id)) {
        downloadedEpisodes.put(kv.getValue().getKey().getEpisodeNumber(), EpisodeObject.builder()
            .episodeNumber(kv.getValue().getKey().getEpisodeNumber())
            .downloadedQuality(kv.getValue().getValue().getDownloadedQuality())
            .downloadTime(kv.getValue().getValue().getDownloadTime()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime()
                .format(downloadTimeFormatter))
            .build());
      }
    });
    model.addAttribute("episodes", downloadedEpisodes.values().stream()
        .sorted(Comparator.comparingLong(EpisodeObject::getEpisodeNumber).reversed())
        .collect(Collectors.toList()));
    return "showEpisodes";
  }

  @RequestMapping(value = "/shows", method = RequestMethod.GET)
  public void showsList(Model model) {
    List<ShowObject> outputList = new ArrayList<>();
    showTable.all(pair -> {
      log.info("Show {}: {}", pair.getValue().getKey().getShowId(),
          Arrays.toString(pair.getKey().getBytes()));
      outputList.add(showMsgToLocalTranslator.translate(pair));
    });
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

    String skipEpisodeString = msg.getValue().getValue().getSkipEpisodeString();
    if (!StringUtils.isEmpty(skipEpisodeString)) {

      Uuid showUuid = showObject.getShowId();
      Set<Long> episodesToPublish = episodesFromRangeCsv(skipEpisodeString);

      episodesToPublish.forEach(episodeNum -> {
        KeyValue<ByteKey<EpisodeMsgKey>, EpisodeMsg> kvPair = downloadedEpisodeMsg(showUuid,
            episodeNum);
        episodeTable.put(kvPair.getKey(), kvPair.getValue());
      });

      msg.getValue().getValue().setSkipEpisodeString(null);
    }

    showTable.put(msg.getKey(), msg.getValue());
    return "redirect:/shows";
  }

  private Set<Long> episodesFromRangeCsv(String rangeCsv) {
    String[] ranges = rangeCsv.split(" *, *");
    HashSet<Long> episodesToPublish = new HashSet<>();
    Pattern rangePattern = Pattern.compile("\\s*(\\d+)\\s*(-\\s*(\\d+)\\s*)?");
    for (String range : ranges) {
      Matcher matcher = rangePattern.matcher(range);
      if (matcher.find()) {
        int startRange = Integer.parseInt(matcher.group(1));
        int endRange = startRange;
        String endRangeString = matcher.group(3);
        if (endRangeString != null) {
          endRange = Integer.parseInt(endRangeString);
        }
        for (long i = startRange; i <= endRange; i++) {
          episodesToPublish.add(i);
        }
      }
    }
    return episodesToPublish;
  }

  private KeyValue<ByteKey<EpisodeMsgKey>, EpisodeMsg> downloadedEpisodeMsg(Uuid showUuid,
      Long episodeNum) {
    EpisodeMsgKey key = EpisodeMsgKey.newBuilder()
        .setEpisodeNumber(episodeNum)
        .setShowId(ShowMsgKey.newBuilder().setShowId(showUuid).build())
        .build();
    EpisodeMsgValue value = EpisodeMsgValue.newBuilder()
        .setMessageId(Uuid.randomUUID())
        .setLatestLinks(new ArrayList<>())
        .setDownloadTime(Instant.now())
        .setDownloadedQuality(-1)
        .build();
    EpisodeMsg msg = EpisodeMsg.newBuilder()
        .setKey(key)
        .setValue(value)
        .build();
    return KeyValue.of(ByteKey.from(key), msg);
  }

  @RequestMapping(path = "/deleteShow/{id}")
  public String deleteShow(@PathVariable final Uuid id, Model model) {
    ByteKey<ShowMsgKey> lookupKey = new ByteKey<>(ShowMsgKey.newBuilder().setShowId(id).build());
    ShowMsg showMessage = showTable.get(lookupKey);
    ShowObject showObject = null;
    if (showMessage != null && showMessage.getValue() != null) {
      showObject = showMsgToLocalTranslator
          .translate(KeyValue.of(lookupKey, showMessage));
    }
    if (showObject != null) {
      showTable.put(lookupKey, null);
      AtomicLong episodeCounter = new AtomicLong(0);
      episodeTable.all(kv -> {
        if (kv.getValue().getKey().getShowId().getShowId().equals(id)) {
          episodeCounter.incrementAndGet();
          episodeTable.put(kv.getKey(), null);
        }
      });
      log.info("Deleted show {} and removed {} associated episodes", id, episodeCounter.get());
    }
    return "redirect:/shows";
  }

  @RequestMapping(path = "/checkShow")
  public String checkShow() {
    showTable.all(pair -> checkShow(pair.getValue().getKey().getShowId()));

    return "redirect:/shows";
  }

  @RequestMapping(path = "/checkShow/{id}")
  public String checkShow(@PathVariable final Uuid id) {
    ByteKey<ShowMsgKey> lookupKey = ByteKey.from(ShowMsgKey.newBuilder().setShowId(id).build());
    ShowMsg showMessage = showTable.get(lookupKey);

    if (showMessage != null) {
      taskExecutor.execute(() -> triggerShowCheckMethod.accept(id));
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

    episodeTable.put(key, null);

    return "redirect:/showEpisodes/" + showId;
  }

}
