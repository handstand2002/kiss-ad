package com.brokencircuits.kissad.showapi.config;

import com.brokencircuits.kissad.kafka.AdminInterface;
import com.brokencircuits.kissad.kafka.ByteKey;
import com.brokencircuits.kissad.kafka.ClusterConnectionProps;
import com.brokencircuits.kissad.kafka.KeyValueStoreWrapper;
import com.brokencircuits.kissad.kafka.Topic;
import com.brokencircuits.kissad.messages.ShowMsg;
import com.brokencircuits.kissad.messages.ShowMsgKey;
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
  Topic<ByteKey<ShowMsgKey>, ShowMsg> showStoreTopic(
      @Value("${messaging.schema-registry-url}") String schemaRegistryUrl) {
    return TopicUtil.showStoreTopic(schemaRegistryUrl);
  }

  @Bean
  KeyValueStoreWrapper<ByteKey<ShowMsgKey>, ShowMsg> showStoreWrapper(
      Topic<ByteKey<ShowMsgKey>, ShowMsg> showStoreTopic) {
    return new KeyValueStoreWrapper<>(STORE_SHOW, showStoreTopic);
  }

  @Bean
  AdminInterface adminInterface(ClusterConnectionProps clusterConnectionProps,
      @Value("${messaging.schema-registry-url}") String schemaRegistryUrl) throws Exception {
    AdminInterface adminInterface = new AdminInterface(schemaRegistryUrl, clusterConnectionProps);
    adminInterface.start();
    return adminInterface;
  }

}
