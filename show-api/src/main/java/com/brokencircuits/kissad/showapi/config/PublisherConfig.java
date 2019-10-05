package com.brokencircuits.kissad.showapi.config;

import com.brokencircuits.kissad.kafka.Publisher;
import com.brokencircuits.kissad.kafka.Topic;
import com.brokencircuits.kissad.messages.ShowMsgKey;
import com.brokencircuits.kissad.messages.ShowMsgValue;
import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig;
import java.util.Properties;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PublisherConfig {

  @Bean
  Publisher<ShowMsgKey, ShowMsgValue> showMessagePublisher(Topic<ShowMsgKey, ShowMsgValue> showTopic,
      Properties producerProperties) {
    return new Publisher<>(producerProperties, showTopic);
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
