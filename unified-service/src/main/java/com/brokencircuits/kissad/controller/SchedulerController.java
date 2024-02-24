package com.brokencircuits.kissad.controller;

import com.brokencircuits.kissad.domain.CheckShowOperation;
import com.brokencircuits.kissad.domain.CheckShowResult;
import com.brokencircuits.kissad.domain.ShowDto;
import com.brokencircuits.kissad.repository.ShowRepository;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SchedulerController {

  private final Map<String, ScheduledFuture<?>> scheduledJobs = new HashMap<>();
  private final TaskScheduler taskScheduler;
  private final CheckShowOperation triggerShowCheckMethod;
  private final ShowRepository showRepository;
  @Value("${scheduler.retry.interval}")
  private Duration retryInterval;
  @Value("${scheduler.retry.count}")
  private int retryCount;

  @PostConstruct
  public void scheduleAll() {
    showRepository.findAll().forEach(this::scheduleShow);
  }

  public synchronized void scheduleShow(ShowDto msg) {
    log.info("Handling update to show {}", msg);
    String showId = msg.getId();
    ScheduledFuture<?> scheduledJob = scheduledJobs.get(showId);
    if (scheduledJob != null) {
      scheduledJob.cancel(false);
      log.info("Cancelled job for show ID: {}", showId);
      scheduledJobs.remove(showId);
    }

    if (msg.getIsActive() != null && msg.getIsActive()) {
      log.info("Scheduling {}", msg);
      try {
        ScheduledFuture<?> job = taskScheduler.schedule(
            () -> {
              log.info("Checking show for new episodes: {}", msg.getTitle());
              CheckShowResult result = null;
              int currentTry = 0;
              int foundEpisodes = 0;
              do {
                if (currentTry > 0) {
                  log.info("Could not find new episodes, waiting {} and trying again for {}",
                      retryInterval, msg.getTitle());
                  sleepThread(retryInterval.toMillis());
                }
                currentTry++;
                try {
                  result = triggerShowCheckMethod.run(UUID.fromString(showId));
                  foundEpisodes = result.getNewEpisodes();
                } catch (Exception e) {
                  log.error("Exception checking for new episodes ", e);
                }

              } while (currentTry < retryCount && (result == null || foundEpisodes == 0));

              log.info("Scheduled job complete - Found {} new episodes for show {}",
                  foundEpisodes, msg.getTitle());
            },
            new CronTrigger(msg.getReleaseScheduleCron()));
        scheduledJobs.put(showId, job);
        log.info("Scheduled check for show {} on schedule {}", msg.getTitle(),
            msg.getReleaseScheduleCron());
      } catch (IllegalArgumentException e) {
        log.warn("Illegal cron expression , not scheduling check for this show: {}", msg);
      }
    }
  }

  private void sleepThread(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }
}
