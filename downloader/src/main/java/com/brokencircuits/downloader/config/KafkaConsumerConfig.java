package com.brokencircuits.downloader.config;

import com.brokencircuits.downloader.messages.DownloadRequestKey;
import com.brokencircuits.downloader.messages.DownloadRequestMsg;
import com.brokencircuits.kissad.kafka.ByteKey;
import com.brokencircuits.kissad.kafka.ClusterConnectionProps;
import com.brokencircuits.kissad.kafka.Topic;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.streams.StreamsConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.listener.ContainerProperties.AckMode;

@EnableKafka
@Configuration
public class KafkaConsumerConfig {

  @Bean
  public KafkaAdmin kafkaAdmin(ClusterConnectionProps props) {
    return new KafkaAdmin(props.asObjectMap());
  }

  @Bean
  public ConsumerFactory<ByteKey<DownloadRequestKey>, DownloadRequestMsg> consumerFactory(
      ClusterConnectionProps kafkaProps,
      Topic<ByteKey<DownloadRequestKey>, DownloadRequestMsg> downloadRequestTopic,
      @Value("${download.downloader-id}") int downloaderId) {
    Map<String, Object> props = new HashMap<>(kafkaProps.getClusterConnection());

    String groupId = kafkaProps.getClusterConnection().get(StreamsConfig.APPLICATION_ID_CONFIG);
    if (groupId == null) {
      throw new IllegalArgumentException("Cluster config " + StreamsConfig.APPLICATION_ID_CONFIG
          + " is required");
    }
    props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId + "-" + downloaderId);
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
    props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

    return new DefaultKafkaConsumerFactory<>(props,
        downloadRequestTopic.getKeySerde().deserializer(),
        downloadRequestTopic.getValueSerde().deserializer());
  }

  @Bean
  public ConcurrentKafkaListenerContainerFactory<ByteKey<DownloadRequestKey>, DownloadRequestMsg> kafkaListenerContainerFactory(
      ConsumerFactory<ByteKey<DownloadRequestKey>, DownloadRequestMsg> consumerFactory) {

    ConcurrentKafkaListenerContainerFactory<ByteKey<DownloadRequestKey>, DownloadRequestMsg> factory
        = new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(consumerFactory);
    factory.getContainerProperties().setAckMode(AckMode.MANUAL);
    factory.setConcurrency(5);

    return factory;
  }

}
