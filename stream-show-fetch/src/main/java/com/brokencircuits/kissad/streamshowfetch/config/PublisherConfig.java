package com.brokencircuits.kissad.streamshowfetch.config;

import com.brokencircuits.kissad.kafka.Publisher;
import com.brokencircuits.kissad.kafka.Topic;
import com.brokencircuits.kissad.messages.DownloadedEpisodeKey;
import com.brokencircuits.kissad.messages.DownloadedEpisodeMessage;
import com.brokencircuits.kissad.messages.KissEpisodePageKey;
import com.brokencircuits.kissad.messages.KissEpisodePageMessage;
import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig;
import java.util.Properties;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PublisherConfig {

  @Bean
  Publisher<KissEpisodePageKey, KissEpisodePageMessage> episodeMessagePublisher(
      Topic<KissEpisodePageKey, KissEpisodePageMessage> topic,
      Properties producerProperties) {
    return new Publisher<>(producerProperties, topic);
  }

  @Bean
  Publisher<DownloadedEpisodeKey, DownloadedEpisodeMessage> downloadedEpisodePublisher(
      Topic<DownloadedEpisodeKey, DownloadedEpisodeMessage> topic,
      Properties producerProperties) {
    return new Publisher<>(producerProperties, topic);
  }

  @Bean
  Properties producerProperties(
      @Value("${messaging.schema-registry-url}") String schemaRegistryUrl,
      @Value("${messaging.brokers}") String brokers) {
    Properties props = new Properties();
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, brokers);
    props.put(ProducerConfig.ACKS_CONFIG, "all");
    props.put(ProducerConfig.BATCH_SIZE_CONFIG, "1");
    props.put(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl);
    return props;
  }
}
