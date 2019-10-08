package com.brokencircuits.kissad.kafka;

import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;

@Slf4j
public abstract class StreamsService {

  private KafkaStreams streams;
  protected StreamsBuilder builder;
  private boolean isRunning = false;

  abstract protected KafkaStreams getStreams();

  protected void afterStreamsStart(KafkaStreams streams) {
    log.info("No actions taken in afterStreamsStart()");
  }

  void start() {
    log.info("Starting streams");
    streams = getStreams();

    AtomicInteger changedToRunningCount = new AtomicInteger(0);
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

  private static String objectClass(Object obj) {
    return obj != null ? obj.getClass().getSimpleName() : "null";
  }
}
