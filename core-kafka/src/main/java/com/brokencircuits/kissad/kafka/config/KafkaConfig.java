package com.brokencircuits.kissad.kafka.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Slf4j
@Component
@RequiredArgsConstructor
@Import(InternalKafkaConfig.class)
@SuppressWarnings("unused")
public class KafkaConfig {

  private final InternalKafkaConfig inner;

  @PostConstruct
  public void logging() {
    StringBuilder sb = new StringBuilder();

    sb.append("\nConsumer:");
    addPropsToLog(sb, consumerProps());
    sb.append("\nProducer");
    addPropsToLog(sb, producerProps());
    sb.append("\nStreams");
    addPropsToLog(sb, streamsProps());
    sb.append("\nAdmin");
    addPropsToLog(sb, adminProps());

    log.info("Effective KafkaConfig: {}", sb);
  }

  private void addPropsToLog(StringBuilder sb, Properties consumerProps) {
    consumerProps.forEach((k, v) -> sb.append("\n\t").append(k).append(": ").append(v));
    if (consumerProps.isEmpty()) {
      sb.append("\n\t --- EMPTY ---");
    }
  }

  public Properties consumerProps() {
    Properties props = new Properties();
    props.putAll(consumerMap());
    return props;
  }

  public Map<String, Object> consumerMap() {
    Map<String, Object> props = new HashMap<>();
    props.putAll(inner.getGlobal());
    props.putAll(inner.getConsumer());

    return props;
  }

  public Properties producerProps() {
    Properties props = new Properties();
    props.putAll(producerMap());
    return props;
  }

  public Map<String, Object> producerMap() {
    Map<String, Object> props = new HashMap<>();
    props.putAll(inner.getGlobal());
    props.putAll(inner.getProducer());

    return props;
  }

  public Properties streamsProps() {
    Properties props = new Properties();
    props.putAll(streamsMap());
    return props;
  }

  public Map<String, Object> streamsMap() {
    Map<String, Object> props = new HashMap<>();
    props.putAll(inner.getGlobal());
    props.putAll(inner.getStreams());

    return props;
  }

  public Properties adminProps() {
    Properties props = new Properties();
    props.putAll(adminMap());
    return props;
  }

  public Map<String, Object> adminMap() {
    Map<String, Object> props = new HashMap<>();
    props.putAll(inner.getGlobal());
    props.putAll(inner.getAdmin());

    return props;
  }
}
