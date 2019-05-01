package com.brokencircuits.kissad.restshow.config;

import com.brokencircuits.kissad.kafka.KeyValueStore;
import com.brokencircuits.kissad.kafka.Topic;
import com.brokencircuits.kissad.kafka.Util;
import com.brokencircuits.kissad.messages.DownloadAvailability;
import com.brokencircuits.kissad.messages.DownloadedEpisodeKey;
import com.brokencircuits.kissad.messages.DownloadedEpisodeMessage;
import com.brokencircuits.kissad.messages.KissShowMessage;
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
  Topic<Long, KissShowMessage> showTopic(
      @Value("${messaging.topics.show}") String topic,
      Serde<KissShowMessage> messageSerde) {
    return new Topic<>(topic, Serdes.Long(), messageSerde);
  }

  @Bean
  Topic<Long, KissShowMessage> showPrivateTopic(
      @Value("${messaging.topics.show-private}") String topic,
      Serde<KissShowMessage> messageSerde) {
    return new Topic<>(topic, Serdes.Long(), messageSerde);
  }

  @Bean
  Serde<KissShowMessage> showMessageSerde(
      @Value("${messaging.schema-registry-url}") String schemaRegistryUrl) {
    return Util.createAvroSerde(schemaRegistryUrl, false);
  }

  @Bean
  Topic<String, Long> urlToShowIdTopic(@Value("${messaging.topics.url-to-show-id}") String topic) {
    return new Topic<>(topic, Serdes.String(), Serdes.Long());
  }

  @Bean
  KeyValueStore<String, Long> showIdLookupStore(
      @Value("${messaging.stores.url-to-show-id}") String storeName) {
    return new KeyValueStore<>(storeName);
  }

  @Bean
  KeyValueStore<Long, KissShowMessage> showMessageStore(
      @Value("${messaging.stores.show}") String storeName) {
    return new KeyValueStore<>(storeName);
  }

  /**
   * Topic for communicating whether or not the downloader can accept new download requests
   */
  @Bean
  Topic<String, DownloadAvailability> downloadAvailabilityTopic(
      @Value("${messaging.topics.downloader-availability}") String topic,
      Serde<DownloadAvailability> msgSerde) {
    return new Topic<>(topic, Serdes.String(), msgSerde);
  }

  @Bean
  Serde<DownloadAvailability> downloadAvailabilitySerde(
      @Value("${messaging.schema-registry-url}") String schemaRegistryUrl) {
    return Util.createAvroSerde(schemaRegistryUrl, false);
  }

  /**
   * Topic for storing the episodes that have been downloaded
   */
  @Bean
  Topic<DownloadedEpisodeKey, DownloadedEpisodeMessage> downloadedEpisodeTopic(
      @Value("${messaging.topics.downloaded-episode}") String topic,
      Serde<DownloadedEpisodeKey> keySerde,
      Serde<DownloadedEpisodeMessage> msgSerde) {
    return new Topic<>(topic, keySerde, msgSerde);
  }

  @Bean
  Serde<DownloadedEpisodeKey> downloadedEpisodeKeySerde(
      @Value("${messaging.schema-registry-url}") String schemaRegistryUrl) {
    return Util.createAvroSerde(schemaRegistryUrl, true);
  }

  @Bean
  Serde<DownloadedEpisodeMessage> downloadedEpisodeMessageSerde(
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
