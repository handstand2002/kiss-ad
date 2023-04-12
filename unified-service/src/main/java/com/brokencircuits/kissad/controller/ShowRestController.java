package com.brokencircuits.kissad.controller;

import com.brokencircuits.kissad.domain.EpisodeDto;
import com.brokencircuits.kissad.domain.EpisodeId;
import com.brokencircuits.kissad.domain.ShowDto;
import com.brokencircuits.kissad.domain.ShowDto.ShowDtoBuilder;
import com.brokencircuits.kissad.domain.rest.CompletedEpisodeDto;
import com.brokencircuits.kissad.domain.rest.SourceName;
import com.brokencircuits.kissad.repository.EpisodeRepository;
import com.brokencircuits.kissad.repository.ShowRepository;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
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
public class ShowRestController {

  private final EpisodeRepository episodeRepository;
  private final ShowRepository showRepository;
  private final Consumer<UUID> triggerShowCheckMethod;
  private final Consumer<ShowDto> onShowUpdate;
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
  public String show(@PathVariable UUID id, Model model) {

    Optional<ShowDto> showDto = showRepository.findById(id.toString());
    if (!showDto.isPresent()) {
      throw new IllegalStateException("Could not find show");
    }

    ShowDto show = showDto.get();
    log.info("Show: {}", show);

    model.addAttribute("sourceTypes", SourceName.values());
    model.addAttribute("show", show);
    return "show";
  }

  @RequestMapping("/showEpisodes/{id}")
  public String showEpisodes(@PathVariable UUID id, Model model) {

    Optional<ShowDto> showDto = showRepository.findById(id.toString());

    model.addAttribute("sourceTypes", SourceName.values());
    showDto.ifPresent(showDbDto -> model.addAttribute("show", showDbDto));

    Map<Long, CompletedEpisodeDto> downloadedEpisodes = new HashMap<>();

    episodeRepository.findByShowId(id.toString())
        .forEach(dto -> downloadedEpisodes.put((long) dto.getEpisodeNumber(),
            CompletedEpisodeDto.builder()
                .downloadedQuality(dto.getDownloadedQuality())
                .downloadTime(dto.getDownloadTime()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime()
                    .format(downloadTimeFormatter))
                .episodeNumber((long) dto.getEpisodeNumber())
                .build()));

    model.addAttribute("episodes", downloadedEpisodes.values().stream()
        .sorted(Comparator.comparingLong(CompletedEpisodeDto::getEpisodeNumber).reversed())
        .collect(Collectors.toList()));
    return "showEpisodes";
  }

  @RequestMapping(value = "/shows", method = RequestMethod.GET)
  public void showsList(Model model) {

    List<ShowDto> sortedShows = showRepository.findAll().stream()
        .sorted(getShowScheduleComparator())
        .map(showDto -> {
          ShowDtoBuilder builder = showDto.toBuilder();
          Date nextRun = nextRunTime(showDto.getReleaseScheduleCron());
          if (nextRun != null) {
            builder.nextEpisode(NEXT_EPISODE_DATE_FORMAT.format(nextRun));
          }
          return builder.build();
        })
        .collect(Collectors.toList());

    log.info("Found {} shows", sortedShows.size());

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

  private static Comparator<ShowDto> getShowScheduleComparator() {
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
  public String addShow(ShowDto showDto, Model model) {

    log.info("Raw show: {}", showDto);
    if (showDto.getIsActive() == null) {
      // if isActive is not present, it should be interpreted as being false
      showDto.setIsActive(false);
    }

    if (showDto.getId() == null) {
      showDto.setId(UUID.randomUUID().toString());
    }

    if (showDto.getEpisodeNamePattern() == null) {
      showDto.setEpisodeNamePattern(defaultEpisodeNamePattern);
    }

    if (showDto.getReleaseScheduleCron() == null) {
      showDto.setReleaseScheduleCron(defaultReleaseScheduleCron);
    }

    String skipEpisodeString = showDto.getSkipEpisodeString();
    if (!StringUtils.isEmpty(skipEpisodeString)) {

      Set<Long> episodesToPublish = episodesFromRangeCsv(skipEpisodeString);

      episodesToPublish.forEach(episodeNum -> {

        episodeRepository.save(EpisodeDto.builder()
            .showId(showDto.getId())
            .episodeNumber(Math.toIntExact(episodeNum))
            .downloadTime(Instant.now())
            .downloadedQuality(-1)
            .build());
      });

      showDto.setSkipEpisodeString(null);
    }

    log.info("Updating show: {}", showDto);

    showRepository.save(showDto);
    onShowUpdate.accept(showDto);
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

  @RequestMapping(path = "/deleteShow/{id}")
  public String deleteShow(@PathVariable final UUID id, Model model) {

    Optional<ShowDto> show = showRepository.findById(id.toString());
    if (show.isPresent()) {
      showRepository.delete(show.get());
      List<EpisodeDto> showId = episodeRepository.findByShowId(show.get().getId());
      AtomicLong episodeCounter = new AtomicLong(0);
      for (EpisodeDto episodeDto : showId) {
        episodeCounter.incrementAndGet();
        episodeRepository.delete(episodeDto);
      }
      onShowUpdate.accept(show.get());
      log.info("Deleted show {} and removed {} associated episodes", id, episodeCounter.get());
    }

    return "redirect:/shows";
  }

  @RequestMapping(path = "/checkShow")
  public String checkShow() {
    for (ShowDto show : showRepository.findAll()) {
      checkShow(UUID.fromString(show.getId()));
    }

    return "redirect:/shows";
  }

  @RequestMapping(path = "/checkShow/{id}")
  public String checkShow(@PathVariable final UUID id) {
    Optional<ShowDto> show = showRepository.findById(id.toString());

    if (show.isPresent()) {
      taskExecutor.execute(() -> triggerShowCheckMethod.accept(id));
    }

    return "redirect:/shows";
  }

  @RequestMapping(path = "/deleteEpisode/{showId}/{epNum}")
  public String deleteEpisode(@PathVariable final UUID showId, @PathVariable final Long epNum) {

    Optional<EpisodeDto> episode = episodeRepository.findById(EpisodeId.builder()
        .showId(showId.toString())
        .episodeNumber(Math.toIntExact(epNum))
        .build());
    episode.ifPresent(episodeRepository::delete);

    return "redirect:/showEpisodes/" + showId;
  }

}
