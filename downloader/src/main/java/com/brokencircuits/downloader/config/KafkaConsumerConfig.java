package com.brokencircuits.downloader.config;

import com.brokencircuits.downloader.messages.DownloadRequestKey;
import com.brokencircuits.downloader.messages.DownloadRequestValue;
import com.brokencircuits.kissad.kafka.KafkaProperties;
import com.brokencircuits.kissad.kafka.Topic;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaAdmin;

@EnableKafka
@Configuration
public class KafkaConsumerConfig {

  @Bean
  public KafkaProperties connectionProperties(@Value("${messaging.brokers}") String brokers) {
    return new KafkaProperties().add(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, brokers);
  }

  @Bean
  public KafkaAdmin kafkaAdmin(KafkaProperties props) {
    return new KafkaAdmin(props.asMap());
  }

  @Bean
  public ConsumerFactory<DownloadRequestKey, DownloadRequestValue> consumerFactory(
      KafkaProperties kafkaProps,
      Topic<DownloadRequestKey, DownloadRequestValue> downloadRequestTopic,
      @Value("${messaging.application-id}") String applicationId,
      @Value("${download.downloader-id}") int downloaderId) {
    Map<String, Object> props = new HashMap<>(kafkaProps.asMap());

    props.put(ConsumerConfig.GROUP_ID_CONFIG, applicationId + "-" + downloaderId);
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
    props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

    return new DefaultKafkaConsumerFactory<>(props,
        downloadRequestTopic.getKeySerde().deserializer(),
        downloadRequestTopic.getValueSerde().deserializer());
  }

  @Bean
  public ConcurrentKafkaListenerContainerFactory<DownloadRequestKey, DownloadRequestValue> kafkaListenerContainerFactory(
      ConsumerFactory<DownloadRequestKey, DownloadRequestValue> consumerFactory) {

    ConcurrentKafkaListenerContainerFactory<DownloadRequestKey, DownloadRequestValue> factory
        = new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(consumerFactory);
    factory.setConcurrency(1);

    return factory;
  }

}
