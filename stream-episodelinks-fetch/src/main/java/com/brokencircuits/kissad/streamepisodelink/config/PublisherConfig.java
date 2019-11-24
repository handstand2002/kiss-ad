package com.brokencircuits.kissad.streamepisodelink.config;

import com.brokencircuits.kissad.kafka.Publisher;
import com.brokencircuits.kissad.kafka.Topic;
import com.brokencircuits.kissad.messages.ExternalEpisodeLinkMessage;
import com.brokencircuits.kissad.messages.KissEpisodePageKey;
import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig;
import java.util.Properties;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PublisherConfig {

  @Bean
  Publisher<ByteKey<KissEpisodePageKey>, ExternalEpisodeLinkMessage> episodeLinkPublisher(
      Topic<ByteKey<KissEpisodePageKey>, ExternalEpisodeLinkMessage> topic,
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
    props.put(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl);
    return props;
  }
}
