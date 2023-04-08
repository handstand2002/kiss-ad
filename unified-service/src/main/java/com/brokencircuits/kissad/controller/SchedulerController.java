package com.brokencircuits.kissad.controller;

import com.brokencircuits.kissad.messages.ShowMsg;
import com.brokencircuits.kissad.messages.ShowMsgKey;
import com.brokencircuits.kissad.table.ReadWriteTable;
import com.brokencircuits.kissad.util.Uuid;
import java.util.HashMap;
import java.util.Map;
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

  private final Map<ShowMsgKey, ScheduledFuture<?>> scheduledJobs = new HashMap<>();
  private final TaskScheduler taskScheduler;
  private final Consumer<Uuid> triggerShowCheckMethod;
  private final ReadWriteTable<ShowMsgKey, ShowMsg> showTable;

  @PostConstruct
  public void scheduleAll() {
    showTable.all(kv -> scheduleShow(kv.getKey(), kv.getValue()));
  }

  public synchronized void scheduleShow(ShowMsgKey key, ShowMsg msg) {
    log.info("Handling update to show {}", msg);
    ScheduledFuture<?> scheduledJob = scheduledJobs.get(key);
    if (scheduledJob != null) {
      scheduledJob.cancel(false);
      log.info("Cancelled job for show ID: {}", key);
      scheduledJobs.remove(key);
    }

    if (msg != null && msg.getValue() != null && msg.getValue().getIsActive()) {
      log.info("Scheduling {}", msg);
      try {
        ScheduledFuture<?> job = taskScheduler.schedule(
            () -> triggerShowCheckMethod.accept(msg.getKey().getShowId()),
            new CronTrigger(msg.getValue().getReleaseScheduleCron()));
        scheduledJobs.put(key, job);
        log.info("Scheduled check for show {} on schedule {}", msg.getValue().getTitle(),
            msg.getValue().getReleaseScheduleCron());
      } catch (IllegalArgumentException e) {
        log.warn("Illegal cron expression , not scheduling check for this show: {} | {}", key, msg);
      }
    }
  }
}
