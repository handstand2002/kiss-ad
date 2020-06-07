package com.brokenciruits.kissad.scheduler.streams;

import com.brokencircuits.kissad.kafka.ByteKey;
import com.brokencircuits.kissad.kafka.KeyValueStoreWrapper;
import com.brokencircuits.kissad.kafka.StreamsService;
import com.brokencircuits.kissad.messages.EpisodeMsg;
import com.brokencircuits.kissad.messages.EpisodeMsgKey;
import com.brokencircuits.kissad.messages.ShowMsg;
import com.brokencircuits.kissad.messages.ShowMsgKey;
import com.brokencircuits.kissad.util.Uuid;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class StreamController extends StreamsService {


  private final KeyValueStoreWrapper<ByteKey<ShowMsgKey>, ShowMsg> showStoreWrapper;
  private final KeyValueStoreWrapper<ByteKey<EpisodeMsgKey>, EpisodeMsg> episodeStoreWrapper;
  private final Collection<KeyValueStoreWrapper<?, ?>> storeWrappers;
  private final TaskScheduler taskScheduler;
  private final Function<Uuid, Boolean> triggerShowMethod;
  private final Map<ByteKey<ShowMsgKey>, ScheduledFuture<?>> scheduledJobs = new HashMap<>();
  private final Map<Uuid, Instant> lastNewEpisodeTriggered;

  @Value("${show.retry.interval:0}")
  private Duration episodeRetryInterval;
  @Value("${show.retry.max-times:0}")
  private int maxRetryCount;

  @Override
  protected void afterStreamsStart(KafkaStreams streams) {
    storeWrappers.forEach(store -> store.initialize(streams));
    try (KeyValueIterator<ByteKey<ShowMsgKey>, ShowMsg> iterator = showStoreWrapper.all()) {
      iterator.forEachRemaining(entry -> scheduleShow(entry.key, entry.value));
    }
  }

  private void scheduleShow(ByteKey<ShowMsgKey> key, ShowMsg msg) {
    log.info("Scheduling {}", msg);
    ScheduledFuture<?> scheduledJob = scheduledJobs.get(key);
    if (scheduledJob != null) {
      scheduledJob.cancel(false);
      log.info("Cancelled job for show ID: {}", key);
      scheduledJobs.remove(key);
    }

    if (msg != null && msg.getValue() != null) {
      try {
        ScheduledFuture<?> job = taskScheduler.schedule(() -> {
          triggerShowMethod.apply(msg.getKey().getShowId());
          scheduleRetries(msg, 0, Instant.now());
        }, new CronTrigger(msg.getValue().getReleaseScheduleCron()));
        scheduledJobs.put(key, job);
        log.info("Scheduled check for show {} on schedule {}", msg.getValue().getTitle(),
            msg.getValue().getReleaseScheduleCron());
      } catch (IllegalArgumentException e) {
        log.warn("Illegal cron expression , not scheduling check for this show: {} | {}", key,
            msg);
      }
    }
  }

  private void scheduleRetries(ShowMsg msg, int usedRetries, Instant initialTrigger) {
    AtomicInteger numRetries = new AtomicInteger(usedRetries);
    taskScheduler.schedule(() -> {
      Instant lastNewEpForShow = lastNewEpisodeTriggered.get(msg.getKey().getShowId());
      boolean hasSeenNewEpisodeTrigger =
          lastNewEpForShow != null && lastNewEpForShow.isAfter(initialTrigger);
      log.info("Checking retry conditions for show {}: numRetries: {}; lastNewEpForShow: {}", msg,
          usedRetries, lastNewEpForShow);
      if (numRetries.incrementAndGet() < maxRetryCount && !hasSeenNewEpisodeTrigger) {
        triggerShowMethod.apply(msg.getKey().getShowId());
        scheduleRetries(msg, numRetries.get(), initialTrigger);
      }
    }, Instant.now().plus(episodeRetryInterval));
  }

  protected Topology buildTopology() {
    StreamsBuilder builder = new StreamsBuilder();

    showStoreWrapper.addToBuilder(builder, this::scheduleShow);
    episodeStoreWrapper.addToBuilder(builder, (key, oldMsg, newMsg) -> {

      // TODO: make the updates here reflect in a map that tells when the latest new episode
      log.info("Received update to episode: {} Old: {}; new: {}", key, oldMsg, newMsg);
    });
    storeWrappers.forEach(wrapper -> wrapper.addToBuilder(builder));

    return builder.build();
  }
}
