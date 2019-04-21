package com.brokencircuits.kissad.streamrvfetch.config;

import com.brokencircuits.kissad.kafka.Topic;
import com.brokencircuits.kissad.kafka.Util;
import com.brokencircuits.kissad.messages.ExternalDownloadLinkKey;
import com.brokencircuits.kissad.messages.ExternalDownloadLinkMessage;
import com.brokencircuits.kissad.messages.ExternalEpisodeLinkMessage;
import com.brokencircuits.kissad.messages.KissEpisodePageKey;
import java.util.Properties;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.streams.StreamsConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaConfig {

  @Bean
  Topic<KissEpisodePageKey, ExternalEpisodeLinkMessage> externalLinkTopic(
      @Value("${messaging.topics.episode-link}") String topic,
      Serde<KissEpisodePageKey> keySerde,
      Serde<ExternalEpisodeLinkMessage> msgSerde) {
    return new Topic<>(topic, keySerde, msgSerde);
  }

  @Bean
  Serde<KissEpisodePageKey> episodeKeySerde(
      @Value("${messaging.schema-registry-url}") String schemaRegistryUrl) {
    return Util.createAvroSerde(schemaRegistryUrl, true);
  }

  @Bean
  Serde<ExternalEpisodeLinkMessage> externalLinkMessageSerde(
      @Value("${messaging.schema-registry-url}") String schemaRegistryUrl) {
    return Util.createAvroSerde(schemaRegistryUrl, false);
  }

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
