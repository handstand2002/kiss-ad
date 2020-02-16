package com.brokenciruits.kissad.scheduler.streams;

import com.brokencircuits.kissad.kafka.ByteKey;
import com.brokencircuits.kissad.kafka.ClusterConnectionProps;
import com.brokencircuits.kissad.kafka.KeyValueStoreWrapper;
import com.brokencircuits.kissad.kafka.StreamsService;
import com.brokencircuits.kissad.messages.ShowMsg;
import com.brokencircuits.kissad.messages.ShowMsgKey;
import com.brokencircuits.kissad.util.Uuid;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Function;
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
  private final KeyValueStoreWrapper<ByteKey<ShowMsgKey>, ShowMsg> showStoreWrapper;
  private final TaskScheduler taskScheduler;
  private final Function<Uuid, Boolean> triggerShowMethod;
  private Map<ByteKey<ShowMsgKey>, ScheduledFuture<?>> scheduledJobs = new HashMap<>();

  @Override
  protected void afterStreamsStart(KafkaStreams streams) {
    showStoreWrapper.initialize(streams);
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
        ScheduledFuture<?> job = taskScheduler
            .schedule(() -> triggerShowMethod.apply(msg.getKey().getShowId()),
                new CronTrigger(msg.getValue().getReleaseScheduleCron()));
        scheduledJobs.put(key, job);
        log.info("Scheduled check for show {} on schedule {}", msg.getValue().getTitle(),
            msg.getValue().getReleaseScheduleCron());
      } catch (IllegalArgumentException e) {
        log.warn("Illegal cron expression , not scheduling check for this show: {} | {}", key,
            msg);
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
