package com.brokencircuits.kissad.streamepisodelink.config;

import com.brokencircuits.kissad.kafka.Topic;
import com.brokencircuits.kissad.kafka.Util;
import com.brokencircuits.kissad.messages.DownloadAvailability;
import com.brokencircuits.kissad.messages.ExternalEpisodeLinkMessage;
import com.brokencircuits.kissad.messages.KissEpisodePageKey;
import com.brokencircuits.kissad.messages.KissEpisodePageMessage;
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
  Topic<KissEpisodePageKey, KissEpisodePageMessage> episodeTopic(
      @Value("${messaging.topics.episode}") String topic,
      Serde<KissEpisodePageKey> keySerde,
      Serde<KissEpisodePageMessage> msgSerde) {
    return new Topic<>(topic, keySerde, msgSerde);
  }

  @Bean
  Serde<KissEpisodePageKey> episodeKeySerde(
      @Value("${messaging.schema-registry-url}") String schemaRegistryUrl) {
    return Util.createAvroSerde(schemaRegistryUrl, true);
  }

  @Bean
  Serde<KissEpisodePageMessage> episodeMessageSerde(
      @Value("${messaging.schema-registry-url}") String schemaRegistryUrl) {
    return Util.createAvroSerde(schemaRegistryUrl, false);
  }

  @Bean
  Topic<KissEpisodePageKey, ExternalEpisodeLinkMessage> externalLinkTopic(
      @Value("${messaging.topics.episode-link}") String topic,
      Serde<KissEpisodePageKey> keySerde,
      Serde<ExternalEpisodeLinkMessage> msgSerde) {
    return new Topic<>(topic, keySerde, msgSerde);
  }

  @Bean
  Serde<ExternalEpisodeLinkMessage> externalLinkMessageSerde(
      @Value("${messaging.schema-registry-url}") String schemaRegistryUrl) {
    return Util.createAvroSerde(schemaRegistryUrl, false);
  }

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
