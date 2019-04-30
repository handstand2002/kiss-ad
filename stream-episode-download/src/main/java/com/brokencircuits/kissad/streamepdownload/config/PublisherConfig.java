package com.brokencircuits.kissad.streamepdownload.config;

import com.brokencircuits.kissad.kafka.Publisher;
import com.brokencircuits.kissad.kafka.Topic;
import com.brokencircuits.kissad.messages.DownloadAvailability;
import com.brokencircuits.kissad.messages.DownloadedEpisodeKey;
import com.brokencircuits.kissad.messages.DownloadedEpisodeMessage;
import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig;
import java.util.Properties;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PublisherConfig {

  @Bean
  Publisher<DownloadedEpisodeKey, DownloadedEpisodeMessage> downloadedEpisodePublisher(
      Topic<DownloadedEpisodeKey, DownloadedEpisodeMessage> topic,
      Properties producerProperties) {
    return new Publisher<>(producerProperties, topic);
  }

  @Bean
  Publisher<String, DownloadAvailability> availabilityPublisher(
      Topic<String, DownloadAvailability> topic,
      Properties producerProperties) {
    return new Publisher<>(producerProperties, topic);
  }

  @Bean
  Properties producerProperties(
      @Value("${messaging.schema-registry-url}") String schemaRegistryUrl,
      @Value("${messaging.brokers}") String brokers,
      @Value("${messaging.application-id}") String applicationId) {
    Properties props = new Properties();
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, brokers);
    props.put(ProducerConfig.CLIENT_ID_CONFIG, applicationId);
    props.put(ProducerConfig.ACKS_CONFIG, "all");
    props.put(ProducerConfig.BATCH_SIZE_CONFIG, "1");
    return props;
  }

}
