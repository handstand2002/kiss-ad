package com.brokencircuits.kissad.streamepdownload.config;

import com.brokencircuits.kissad.kafka.Topic;
import com.brokencircuits.kissad.kafka.Util;
import com.brokencircuits.kissad.messages.DownloadAvailability;
import com.brokencircuits.kissad.messages.DownloadedEpisodeKey;
import com.brokencircuits.kissad.messages.DownloadedEpisodeMessage;
import com.brokencircuits.kissad.messages.ExternalDownloadLinkKey;
import com.brokencircuits.kissad.messages.ExternalDownloadLinkMessage;
import java.util.Properties;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaConfig {

  /**
   * Topic that contains download links for this module to download
   */
  @Bean
  Topic<ExternalDownloadLinkKey, ExternalDownloadLinkMessage> topic(
      @Value("${messaging.topics.episode-download-link}") String topic,
      Serde<ExternalDownloadLinkKey> keySerde,
      Serde<ExternalDownloadLinkMessage> msgSerde) {
    return new Topic<>(topic, keySerde, msgSerde);
  }

  @Bean
  Serde<ExternalDownloadLinkKey> externalDownloadLinkKeySerde(
      @Value("${messaging.schema-registry-url}") String schemaRegistryUrl) {
    return Util.createAvroSerde(schemaRegistryUrl, true);
  }

  @Bean
  Serde<ExternalDownloadLinkMessage> externalDownloadLinkMessageSerde(
      @Value("${messaging.schema-registry-url}") String schemaRegistryUrl) {
    return Util.createAvroSerde(schemaRegistryUrl, false);
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
      @Value("${messaging.brokers}") String brokers) {

    Properties props = new Properties();
    props.put(StreamsConfig.APPLICATION_ID_CONFIG, applicationId);
    props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, brokers);
    return props;
  }

}
