package com.brokencircuits.kissad.hsshowfetch.config;

import com.brokencircuits.kissad.kafka.KeyValueStore;
import com.brokencircuits.kissad.kafka.Topic;
import com.brokencircuits.kissad.kafka.Util;
import com.brokencircuits.kissad.messages.EpisodeKey;
import com.brokencircuits.kissad.messages.EpisodeLinks;
import com.brokencircuits.kissad.messages.ShowMessage;
import java.util.Properties;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaConfig {

  @Bean
  Topic<Long, ShowMessage> showTopic(
      @Value("${messaging.topics.show-store}") String topic,
      Serde<ShowMessage> messageSerde) {
    return new Topic<>(topic, Serdes.Long(), messageSerde);
  }

  @Bean
  Serde<ShowMessage> showMessageSerde(
      @Value("${messaging.schema-registry-url}") String schemaRegistryUrl) {
    return Util.createAvroSerde(schemaRegistryUrl, false);
  }

  @Bean
  KeyValueStore<Long, ShowMessage> showStore(
      @Value("${messaging.stores.show}") String storeName,
      Topic<Long, ShowMessage> showTopic) {
    return new KeyValueStore<>(storeName, showTopic);
  }

  /**
   * Topic for triggering new download of episodes
   */
  @Bean
  Topic<EpisodeKey, EpisodeLinks> episodeQueueTopic(
      @Value("${messaging.topics.episode-queue}") String topic,
      Serde<EpisodeKey> keySerde, Serde<EpisodeLinks> msgSerde) {
    return new Topic<>(topic, keySerde, msgSerde);
  }

  @Bean
  Serde<EpisodeKey> episodeKeySerde(
      @Value("${messaging.schema-registry-url}") String schemaRegistryUrl) {
    return Util.createAvroSerde(schemaRegistryUrl, true);
  }

  @Bean
  Serde<EpisodeLinks> episodeLinksSerde(
      @Value("${messaging.schema-registry-url}") String schemaRegistryUrl) {
    return Util.createAvroSerde(schemaRegistryUrl, false);
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
