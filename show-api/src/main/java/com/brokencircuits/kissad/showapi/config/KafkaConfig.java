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

  public static final String TOPIC_IN = "inTopic";
  public static final String TOPIC_PART1 = "repartTopic1";
  public static final String TOPIC_PART2 = "repartTopic2";
  public static final String TOPIC_PART3 = "repartTopic3";
  public static final String TOPIC_OUT = "outTopic";


  @Bean
  Topic<String, String> inTopic(
      @Value("${messaging.schema-registry-url}") String schemaRegistryUrl) {
    return new Topic<>(TOPIC_IN, Serdes.String(), Serdes.String());
  }

  @Bean
  Topic<String, String> repartition1Topic(
      @Value("${messaging.schema-registry-url}") String schemaRegistryUrl) {
    return new Topic<>(TOPIC_PART1, Serdes.String(), Serdes.String());
  }

  @Bean
  Topic<String, String> repartition2Topic(
      @Value("${messaging.schema-registry-url}") String schemaRegistryUrl) {
    return new Topic<>(TOPIC_PART2, Serdes.String(), Serdes.String());
  }

  @Bean
  Topic<String, String> repartition3Topic(
      @Value("${messaging.schema-registry-url}") String schemaRegistryUrl) {
    return new Topic<>(TOPIC_PART3, Serdes.String(), Serdes.String());
  }

  @Bean
  Topic<String, String> outTopic(
      @Value("${messaging.schema-registry-url}") String schemaRegistryUrl) {
    return new Topic<>(TOPIC_OUT, Serdes.String(), Serdes.String());
  }

}
