package com.brokencircuits.kissad.schemaupdater.config;

import static com.brokencircuits.kissad.topics.TopicUtil.showStoreTopic;

import com.brokencircuits.kissad.kafka.ClusterConnectionProps;
import com.brokencircuits.kissad.kafka.Topic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaConfig {

  @Bean
  @ConfigurationProperties(prefix = "messaging")
  ClusterConnectionProps clusterConnectionProps() {
    return new ClusterConnectionProps();
  }

  @Bean
  public Topic<?, ?> showTopic(
      @Value("${messaging.schema-registry-url}") String schemaRegistryUrl) {
    return showStoreTopic(schemaRegistryUrl);
  }
}
