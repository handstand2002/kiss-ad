package com.brokencircuits.kissad.schemaupdater.config;

import com.brokencircuits.kissad.kafka.ClusterConnectionProps;
import com.brokencircuits.kissad.kafka.Topic;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties.AckMode;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.support.TopicPartitionInitialOffset;
import org.springframework.kafka.support.TopicPartitionInitialOffset.SeekPosition;

@Slf4j
@Configuration
public class KafkaConsumerConfig {

  @Bean
  public ConsumerFactory<byte[], byte[]> consumerFactory(
      ClusterConnectionProps clusterConnectionProps) {

    Map<String, Object> propMap = clusterConnectionProps.asObjectMap();
    propMap.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

    return new DefaultKafkaConsumerFactory<>(propMap, Serdes.ByteArray().deserializer(),
        Serdes.ByteArray().deserializer());
  }

  @Bean
  public ConcurrentKafkaListenerContainerFactory<byte[], byte[]> kafkaListenerContainerFactory(
      ConsumerFactory<byte[], byte[]> consumerFactory) {

    ConcurrentKafkaListenerContainerFactory<byte[], byte[]> factory =
        new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(consumerFactory);
    factory.getContainerProperties().setAckMode(AckMode.MANUAL);

    return factory;
  }

  @Bean
  public ConcurrentMessageListenerContainer<byte[], byte[]> startContainer(
      ConcurrentKafkaListenerContainerFactory<byte[], byte[]> kafkaListenerContainerFactory,
      Collection<Topic<SpecificRecord, ?>> topics,
      KafkaTemplate<byte[], byte[]> kafkaTemplate) {

    Consumer<? super byte[], ? super byte[]> consumer = kafkaListenerContainerFactory
        .getConsumerFactory().createConsumer();

    Collection<PartitionInfo> allPartitions = new HashSet<>();
    for (Topic<?, ?> topic : topics) {
      allPartitions.addAll(consumer.partitionsFor(topic.getName()));
    }
    consumer.close();

    if (topics.isEmpty()) {
      throw new IllegalStateException("No Topics available");
    }
    Topic<SpecificRecord, ?> firstTopic = topics.iterator().next();

    Serde<SpecificRecord> keySerde = firstTopic.getKeySerde();

    ConcurrentMessageListenerContainer<byte[], byte[]> container = kafkaListenerContainerFactory
        .createContainer(partitionsAtBeginning(allPartitions));

    container.getContainerProperties().setMessageListener(
        (MessageListener<byte[], byte[]>) record -> {

          if (record.value() == null) {
            return;
          }
          String topic = record.topic();
          SpecificRecord deserializedKey = keySerde.deserializer().deserialize(topic, record.key());
          byte[] reSerializedKey = keySerde.serializer().serialize(topic, deserializedKey);

          log.info("Record: {} | {}", record.key(), record.value());
          if (!Arrays.equals(reSerializedKey, record.key())) {
            kafkaTemplate.send(topic, record.key(), null);
            kafkaTemplate.send(topic, reSerializedKey, record.value());
            log.info("Republished record under new schema ID with key: {}", deserializedKey);
          }
        });

    container.start();

    return container;
  }

  private Collection<TopicPartitionInitialOffset> partitionsAtBeginning(
      Collection<PartitionInfo> partitions) {
    Collection<TopicPartitionInitialOffset> partitionsAtBeginning = new HashSet<>();
    for (PartitionInfo partition : partitions) {
      partitionsAtBeginning.add(
          new TopicPartitionInitialOffset(partition.topic(), partition.partition(),
              SeekPosition.BEGINNING));
    }
    return partitionsAtBeginning;
  }
}
