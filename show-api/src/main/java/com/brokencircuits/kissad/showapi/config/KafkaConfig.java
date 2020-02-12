package com.brokencircuits.kissad.showapi.config;

import com.brokencircuits.kissad.kafka.ClusterConnectionProps;
import com.brokencircuits.kissad.kafka.Topic;
import org.apache.kafka.common.serialization.Serdes;
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
  Topic<String, String> inTopic(
      @Value("${messaging.schema-registry-url}") String schemaRegistryUrl) {
    return new Topic<>("inTopic", Serdes.String(), Serdes.String());
  }

  @Bean
  Topic<String, String> repartition1Topic(
      @Value("${messaging.schema-registry-url}") String schemaRegistryUrl) {
    return new Topic<>("repartTopic1", Serdes.String(), Serdes.String());
  }

  @Bean
  Topic<String, String> repartition2Topic(
      @Value("${messaging.schema-registry-url}") String schemaRegistryUrl) {
    return new Topic<>("repartTopic2", Serdes.String(), Serdes.String());
  }

  @Bean
  Topic<String, String> repartition3Topic(
      @Value("${messaging.schema-registry-url}") String schemaRegistryUrl) {
    return new Topic<>("repartTopic3", Serdes.String(), Serdes.String());
  }

  @Bean
  Topic<String, String> outTopic(
      @Value("${messaging.schema-registry-url}") String schemaRegistryUrl) {
    return new Topic<>("outTopic", Serdes.String(), Serdes.String());
  }

}
