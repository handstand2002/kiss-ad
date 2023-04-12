package com.brokencircuits.kissad.controller;

import com.brokencircuits.kissad.domain.ShowDto;
import com.brokencircuits.kissad.repository.ShowRepository;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Consumer;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SchedulerController {

  private final Map<String, ScheduledFuture<?>> scheduledJobs = new HashMap<>();
  private final TaskScheduler taskScheduler;
  private final Consumer<UUID> triggerShowCheckMethod;
  private final ShowRepository showRepository;

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
            () -> triggerShowCheckMethod.accept(UUID.fromString(showId)),
            new CronTrigger(msg.getReleaseScheduleCron()));
        scheduledJobs.put(showId, job);
        log.info("Scheduled check for show {} on schedule {}", msg.getTitle(),
            msg.getReleaseScheduleCron());
      } catch (IllegalArgumentException e) {
        log.warn("Illegal cron expression , not scheduling check for this show: {}", msg);
      }
    }
  }
}
