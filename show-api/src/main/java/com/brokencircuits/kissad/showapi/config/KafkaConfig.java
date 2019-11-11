package com.brokencircuits.kissad.showapi.config;

import com.brokencircuits.kissad.kafka.ClusterConnectionProps;
import com.brokencircuits.kissad.kafka.KeyValueStoreWrapper;
import com.brokencircuits.kissad.kafka.Topic;
import com.brokencircuits.kissad.kafka.TopicKeyUpdater;
import com.brokencircuits.kissad.messages.ShowMsgKey;
import com.brokencircuits.kissad.messages.ShowMsgValue;
import com.brokencircuits.kissad.topics.TopicUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaConfig {

  public static final String STORE_SHOW = "show";

  @Bean
  @ConfigurationProperties(prefix = "messaging")
  ClusterConnectionProps clusterConnectionProps() {
    return new ClusterConnectionProps();
  }

  @Bean
  Topic<ShowMsgKey, ShowMsgValue> showStoreTopic(
      @Value("${messaging.schema-registry-url}") String schemaRegistryUrl) {
    return TopicUtil.showStoreTopic(schemaRegistryUrl);
  }

  @Bean
  TopicKeyUpdater<ShowMsgKey, ShowMsgValue> showKeyUpdater(
      ClusterConnectionProps clusterConnectionProps, Topic<ShowMsgKey, ShowMsgValue> showTopic)
      throws Exception {
    return new TopicKeyUpdater<>(showTopic, clusterConnectionProps);
  }

  @Bean
  KeyValueStoreWrapper<ShowMsgKey, ShowMsgValue> showStoreWrapper(
      Topic<ShowMsgKey, ShowMsgValue> showStoreTopic) {
    return new KeyValueStoreWrapper<>(STORE_SHOW, showStoreTopic);
  }

}
