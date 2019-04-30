package com.brokencircuits.kissad.restshow.poll;

import com.brokencircuits.kissad.kafka.KeyValueStore;
import com.brokencircuits.kissad.kafka.Publisher;
import com.brokencircuits.kissad.messages.KissShowMessage;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PollNewEpisodeScheduleController {

  final private TaskScheduler taskScheduler;
  final private KeyValueStore<Long, KissShowMessage> showMessageStore;
  final private Publisher<Long, KissShowMessage> showMessagePublisher;

  final private AtomicBoolean downloaderBusy = new AtomicBoolean(false);
  final private AtomicBoolean pollQueued = new AtomicBoolean(false);
  private LocalDateTime lastDownloaderStatusChangeTime = LocalDateTime.now().minusDays(1);
  private LocalDateTime lastPollSubmitTime = LocalDateTime.now().minusDays(1);
  private ScheduledFuture<?> futureSubmit;

  final private static Duration downloaderIdleTimeout = Duration.ofSeconds(15);
  final private static Duration timeForRequestToGetToDownloader = Duration.ofSeconds(20);
  final private static Duration singleRequestAllowedPerTime = Duration.ofSeconds(60);
  final private Runnable clearPollQueueRunner = () -> {
    trySendPoll(true);
    pollQueued.set(false);
  };

  public PollNewEpisodeScheduleController(TaskScheduler taskScheduler,
      KeyValueStore<Long, KissShowMessage> showMessageStore,
      Publisher<Long, KissShowMessage> showMessagePublisher,
      List<String> newEpisodePollSchedule) {
    this.taskScheduler = taskScheduler;
    this.showMessageStore = showMessageStore;
    this.showMessagePublisher = showMessagePublisher;

    newEpisodePollSchedule.forEach(scheduleString -> {
      log.info("Creating poll schedule: {}", scheduleString);
      taskScheduler
          .schedule(() -> trySendPoll(false), new CronTrigger(scheduleString));
    });
  }

  /**
   * Try to send poll of episodes. If the downloader isn't idle though, it will mark that there is a
   * poll pending. The next time the downloader changes to available (and idle), it will send the
   * poll
   */
  public boolean trySendPoll(boolean overrideSend) {
    log.info("Trying to send poll for new episodes. {}", overrideSend ? "OVERRIDE" : "");

    boolean isRequestDelayedEnough = LocalDateTime.now()
        .isAfter(lastPollSubmitTime.plus(singleRequestAllowedPerTime));
    boolean isDownloaderIdle = LocalDateTime.now().isAfter(lastDownloaderStatusChangeTime.plus(
        downloaderIdleTimeout));

    if (overrideSend || (!downloaderBusy.get() && isRequestDelayedEnough && isDownloaderIdle)) {
      log.info("Sending poll request. Reason: {}", overrideSend ? "Override" : "Idle");
      // send poll
      submitPollForNewEpisodes();
      return true;
    } else {
      log.info("Can't send poll request, queueing it");
      pollQueued.set(true);
      if (!isRequestDelayedEnough) {
        schedulePoll(Instant.now().plus(singleRequestAllowedPerTime));
      }
      return false;
    }
  }

  /**
   * Streams can tell this controller when the downloader changes status, so this can control when
   * to send poll requests
   */
  public void setDownloaderBusy(boolean isBusy) {
    if (isBusy != downloaderBusy.get()) {
      log.info("Received signal from downloader that it is {}", isBusy ? "busy" : "available");
      downloaderBusy.set(isBusy);
      if (isBusy && futureSubmit != null) {
        log.info("Cancelling previous poll request that was going to send");
        futureSubmit.cancel(false);
        futureSubmit = null;
      }

      if (!downloaderBusy.get() && pollQueued.get()) {
        log.info("downloader isn't busy and there is a poll queued");
        schedulePoll(Instant.now().plus(timeForRequestToGetToDownloader));
      }
    }
  }

  private void schedulePoll(Instant atTime) {
    if (futureSubmit != null) {
      futureSubmit.cancel(false);
    }
    log.info("Cancelling any previous scheduling and scheduling poll to happen at {}", atTime);
    futureSubmit = taskScheduler.schedule(clearPollQueueRunner, atTime);
  }

  private void submitPollForNewEpisodes() {
    log.info("Submitting poll for new episodes");
    lastPollSubmitTime = LocalDateTime.now();
    KeyValueIterator<Long, KissShowMessage> iter = showMessageStore.getStore().all();
    while (iter.hasNext()) {
      KeyValue<Long, KissShowMessage> entry = iter.next();
      if (entry.value.getIsActive()) {
        // this field is only used for initial publish
        entry.value.setSkipEpisodeString(null);
        showMessagePublisher.send(entry);
      }
    }
  }

}
