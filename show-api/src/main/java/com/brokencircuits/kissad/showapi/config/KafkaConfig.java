package com.brokencircuits.kissad.showapi.config;

import com.brokencircuits.kissad.kafka.KeyValueStoreWrapper;
import com.brokencircuits.kissad.kafka.Topic;
import com.brokencircuits.kissad.messages.ShowMsgKey;
import com.brokencircuits.kissad.messages.ShowMsgValue;
import com.brokencircuits.kissad.topics.TopicUtil;
import java.util.Properties;
import org.apache.kafka.streams.StreamsConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaConfig {

  public static final String STORE_SHOW = "show";

  @Bean
  Topic<ShowMsgKey, ShowMsgValue> showStoreTopic(
      @Value("${messaging.schema-registry-url}") String schemaRegistryUrl) {
    return TopicUtil.showStoreTopic(schemaRegistryUrl);
  }

  @Bean
  KeyValueStoreWrapper<ShowMsgKey, ShowMsgValue> showStoreWrapper(
      Topic<ShowMsgKey, ShowMsgValue> showStoreTopic) {
    return new KeyValueStoreWrapper<>(STORE_SHOW, showStoreTopic);
  }

  @Bean
  Properties streamProperties(
      @Value("${messaging.application-id}") String applicationId,
      @Value("${messaging.brokers}") String brokers,
      @Value("${messaging.state-dir}") String stateDir) {

    Properties props = new Properties();
    props.put(StreamsConfig.APPLICATION_ID_CONFIG, applicationId);
    props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, brokers);
    props.put(StreamsConfig.STATE_DIR_CONFIG, stateDir);
    return props;
  }

}
