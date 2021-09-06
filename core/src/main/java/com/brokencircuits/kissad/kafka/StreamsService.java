package com.brokencircuits.kissad.kafka;

import com.brokencircuits.kissad.kafka.config.KafkaConfig;
import java.io.Closeable;
import java.io.IOException;
import java.time.Duration;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.Topology;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Slf4j
@Import(KafkaConfig.class)
public abstract class StreamsService implements Closeable {

  @Autowired
  private KafkaConfig kafkaConfig;

  private KafkaStreams streams;
  private boolean isRunning = false;

  abstract protected Topology buildTopology();

  protected void afterStreamsStart(KafkaStreams streams) {
    log.info("No actions taken in afterStreamsStart()");
  }

  @Override
  public void close() throws IOException {
    if (streams != null && isRunning) {
      streams.close(Duration.ofSeconds(60));
    }
  }

  @PostConstruct
  void start() {
    log.info("Starting streams");
    streams = new KafkaStreams(buildTopology(), kafkaConfig.streamsProps());

    streams.start();
    isRunning = true;
    afterStreamsStart(streams);
  }

  void stop() {
    try {
      close();
    } catch (IOException e) {
      log.error("Unable to stop streams properly: ", e);
      throw new RuntimeException(e);
    }
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
