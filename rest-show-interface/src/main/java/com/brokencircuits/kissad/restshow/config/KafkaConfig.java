package com.brokencircuits.kissad.restshow.config;

import com.brokencircuits.kissad.kafka.Topic;
import com.brokencircuits.kissad.messages.KissShowMessage;
import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import java.io.File;
import java.util.Collections;
import java.util.Properties;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.FileSystemUtils;

@Configuration
public class KafkaConfig {

  @Bean
  Topic<Long, KissShowMessage> showTopic(
      @Value("${messaging.topics.show}") String topic,
      Serde<KissShowMessage> messageSerde) {
    return new Topic<>(topic, Serdes.Long(), messageSerde);
  }

  @Bean
  Serde<KissShowMessage> showMessageSerde(
      @Value("${messaging.schema-registry-url}") String schemaRegistryUrl) {
    return createSerde(schemaRegistryUrl, false);
  }

  @Bean
  Properties streamProperties(
      @Value("${messaging.application-id}") String applicationId,
      @Value("${messaging.brokers}") String brokers,
      @Value("${messaging.state-dir}") String stateDir,
      @Value("${messaging.purge-state-dir-before-start}") boolean purgeStateDir) {

    if (purgeStateDir) {
      FileSystemUtils.deleteRecursively(new File(stateDir));
    }
    Properties props = new Properties();
    props.put(StreamsConfig.APPLICATION_ID_CONFIG, applicationId);
    props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, brokers);
    props.put(StreamsConfig.STATE_DIR_CONFIG, stateDir);
    return props;
  }

  private <T extends SpecificRecord> Serde<T> createSerde(String schemaRegistryUrl,
      boolean forKey) {
    final SpecificAvroSerde<T> serde = new SpecificAvroSerde<>();
    serde.configure(Collections
            .singletonMap(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl),
        forKey);
    return serde;
  }


}
