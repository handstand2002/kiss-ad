package com.brokencircuits.kissad.schemaupdater.config;

import com.brokencircuits.kissad.kafka.ClusterConnectionProps;
import java.util.Map;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

@Configuration
public class KafkaProducerConfig {

  @Bean
  public ProducerFactory<byte[], byte[]> producerFactory(
      ClusterConnectionProps clusterConnectionProps) {
    Map<String, Object> kafkaProps = clusterConnectionProps.asObjectMap();
    kafkaProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);
    kafkaProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);
    return new DefaultKafkaProducerFactory<>(kafkaProps);
  }

  @Bean
  public KafkaTemplate<byte[], byte[]> kafkaTemplate(
      ProducerFactory<byte[], byte[]> producerFactory) {
    return new KafkaTemplate<>(producerFactory);
  }
}