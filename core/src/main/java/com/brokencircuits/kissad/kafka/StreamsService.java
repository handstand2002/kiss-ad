package com.brokencircuits.kissad.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.Topology;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Slf4j
@Import({StreamsServiceRunner.class, ClusterConnectionProps.class})
public abstract class StreamsService {

  @Autowired
  private ClusterConnectionProps clusterConnectionProps;

  private KafkaStreams streams;
  private boolean isRunning = false;

  abstract protected Topology buildTopology();

  protected void afterStreamsStart(KafkaStreams streams) {
    log.info("No actions taken in afterStreamsStart()");
  }

  void start() {
    log.info("Starting streams");
    streams = new KafkaStreams(buildTopology(), clusterConnectionProps.asProperties());

    streams.setStateListener((newState, oldState) -> {
      if (newState.isRunning() && !isRunning) {
        try {
          log.info("Trying to run afterStreamsStart()");
          afterStreamsStart(streams);
          log.info("afterStreamsStart completed successfully");
          isRunning = true;
        } catch (RuntimeException e) {
          log.info("afterStreamsStart failed with exception message: {}", e.getMessage());
        }
      }
    });

    streams.start();
  }

  void stop() {
    streams.close();
  }

  public static void logConsume(Object key, Object value) {
    log.info("Consumed [{}|{}]: {} | {}", objectClass(key), objectClass(value), key, value);
  }

  public static void logProduce(Object key, Object value) {
    log.info("Produced [{}|{}]: {} | {}", objectClass(key), objectClass(value), key, value);
  }

  private static String objectClass(Object obj) {
    return obj != null ? obj.getClass().getSimpleName() : "null";
  }
}
