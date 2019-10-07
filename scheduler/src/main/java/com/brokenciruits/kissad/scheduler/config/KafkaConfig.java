package com.brokenciruits.kissad.scheduler.config;

import com.brokencircuits.kissad.kafka.KafkaProperties;
import com.brokencircuits.kissad.kafka.KeyValueStoreWrapper;
import com.brokencircuits.kissad.kafka.Topic;
import com.brokencircuits.kissad.messages.ShowMsgKey;
import com.brokencircuits.kissad.messages.ShowMsgValue;
import com.brokencircuits.kissad.topics.TopicUtil;
import com.brokenciruits.kissad.scheduler.kafka.StreamProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class KafkaConfig {

  public static final String STORE_SHOW = "show";

  @Bean
  KafkaProperties kafkaProperties(StreamProperties streamProperties) {
    KafkaProperties props = new KafkaProperties();
    streamProperties.getUpdatedStreamConfig().forEach(props::add);
    return props;
  }

  @Bean
  KeyValueStoreWrapper<ShowMsgKey, ShowMsgValue> showStoreWrapper(
      Topic<ShowMsgKey, ShowMsgValue> showStoreTopic) {
    return new KeyValueStoreWrapper<>(STORE_SHOW, showStoreTopic);
  }

  @Bean
  Topic<ShowMsgKey, ShowMsgValue> showStoreTopic(
      @Value("${messaging.schema-registry-url}") String schemaRegistryUrl) {
    return TopicUtil.showStoreTopic(schemaRegistryUrl);
  }

  @Bean
  Topic<ShowMsgKey, ShowMsgValue> showQueueTopic(
      @Value("${messaging.schema-registry-url}") String schemaRegistryUrl) {
    return TopicUtil.showQueueTopic(schemaRegistryUrl);
  }

}
