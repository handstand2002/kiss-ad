package com.brokencircuits.kissad.kafka;

import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;

@Slf4j
public abstract class StreamsService {

  private KafkaStreams streams;
  protected StreamsBuilder streamsBuilder = new StreamsBuilder();

  abstract protected KafkaStreams getStreams();

  protected void afterStreamsStart(KafkaStreams streams) {
    log.info("No actions taken in afterStreamsStart()");
  }

  void start() {
    log.info("Starting streams");
    streams = getStreams();

    AtomicInteger changedToRunningCount = new AtomicInteger(0);
    streams.setStateListener((newState, oldState) -> {
      if (newState.isRunning()) {
        changedToRunningCount.incrementAndGet();
        if (changedToRunningCount.get() == 2) {
          log.info("Streams fully initialized, running afterStreamsStart()");
          afterStreamsStart(streams);
        }
      }
    });

    streams.start();
  }

  void stop() {
    streams.close();
  }
}
