package com.brokenciruits.kissad.scheduler.streams;

import com.brokencircuits.kissad.kafka.ClusterConnectionProps;
import com.brokencircuits.kissad.kafka.KeyValueStoreWrapper;
import com.brokencircuits.kissad.kafka.Publisher;
import com.brokencircuits.kissad.kafka.StreamsService;
import com.brokencircuits.kissad.messages.ShowMsgKey;
import com.brokencircuits.kissad.messages.ShowMsgValue;
import com.brokencircuits.kissad.util.Uuid;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.function.BiConsumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class StreamController extends StreamsService {

  private final ClusterConnectionProps clusterConnectionProps;
  private final KeyValueStoreWrapper<ShowMsgKey, ShowMsgValue> showStoreWrapper;
  private final TaskScheduler taskScheduler;
  private final Publisher<ShowMsgKey, ShowMsgValue> showTriggerPublisher;
  private Map<Uuid, ScheduledFuture<?>> scheduledJobs = new HashMap<>();

  private final BiConsumer<ShowMsgKey, ShowMsgValue> onSchedule = new BiConsumer<ShowMsgKey, ShowMsgValue>() {
    @Override
    public void accept(ShowMsgKey key, ShowMsgValue value) {
      log.info("Triggered show {} | {}", key, value);
      value.setMessageId(Uuid.randomUUID());
      showTriggerPublisher.send(key, value);
    }
  };

  @Override
  protected void afterStreamsStart(KafkaStreams streams) {
    showStoreWrapper.initialize(streams);
    try (KeyValueIterator<ShowMsgKey, ShowMsgValue> iterator = showStoreWrapper.all()) {
      iterator.forEachRemaining(entry -> scheduleShow(entry.key, entry.value));
    }
  }

  private void scheduleShow(ShowMsgKey key, ShowMsgValue value) {
    log.info("Scheduling {} | {}", key, value);
    ScheduledFuture<?> scheduledJob = scheduledJobs.get(key.getShowId());
    if (scheduledJob != null) {
      scheduledJob.cancel(false);
      log.info("Cancelled job for show ID: {}", key.getShowId());
      scheduledJobs.remove(key.getShowId());
    }

    if (value != null) {
      try {
        ScheduledFuture<?> job = taskScheduler.schedule(() -> onSchedule.accept(key, value),
            new CronTrigger(value.getReleaseScheduleCron()));
        scheduledJobs.put(key.getShowId(), job);
        log.info("Scheduled check for show {} on schedule {}", value.getTitle(),
            value.getReleaseScheduleCron());
      } catch (IllegalArgumentException e) {
        log.warn("Illegal cron expression , not scheduling check for this show: {} | {}", key,
            value);
      }
    }
  }

  @Override
  protected KafkaStreams getStreams() {
    return new KafkaStreams(buildTopology(), clusterConnectionProps.asProperties());
  }

  private Topology buildTopology() {
    builder = new StreamsBuilder();

    showStoreWrapper.addToBuilder(builder, this::scheduleShow);

    return builder.build();
  }
}
