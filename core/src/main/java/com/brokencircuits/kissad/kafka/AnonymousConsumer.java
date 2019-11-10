package com.brokencircuits.kissad.kafka;

import com.brokencircuits.kissad.util.Service;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties.AckMode;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.support.TopicPartitionInitialOffset;
import org.springframework.kafka.support.TopicPartitionInitialOffset.SeekPosition;

@Slf4j
public class AnonymousConsumer<K, V> implements Service {

  private final Topic<K, V> topic;
  private final ClusterConnectionProps kafkaProperties;
  private final ConcurrentKafkaListenerContainerFactory<K, V> containerFactory;
  private ConcurrentMessageListenerContainer<K, V> container;
  @Setter
  private MessageListener<K, V> messageListener;

  public AnonymousConsumer(Topic<K, V> topic, ClusterConnectionProps kafkaProperties) {
    this.topic = topic;
    this.kafkaProperties = kafkaProperties;

    containerFactory = kafkaListenerContainerFactory(topic, kafkaProperties.asObjectMap());
    messageListener = record -> log.info("Received record {}", record);
    log.info("[{}] State: CREATED", topic.getName());
  }

  private static <K, V> ConsumerFactory<K, V> consumerFactory(Topic<K, V> topic,
      Map<String, Object> kafkaProps) {

    kafkaProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
    kafkaProps.remove(ConsumerConfig.GROUP_ID_CONFIG);

    return new DefaultKafkaConsumerFactory<>(kafkaProps, topic.getKeySerde().deserializer(),
        topic.getValueSerde().deserializer());
  }

  private static <K, V> ConcurrentKafkaListenerContainerFactory<K, V> kafkaListenerContainerFactory(
      Topic<K, V> topic, Map<String, Object> kafkaProps) {

    ConcurrentKafkaListenerContainerFactory<K, V> factory = new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(consumerFactory(topic, kafkaProps));
    factory.getContainerProperties().setAckMode(AckMode.MANUAL);

    return factory;
  }

  private static Collection<TopicPartitionInitialOffset> partitionsAtBeginning(
      Collection<PartitionInfo> partitions) {
    Collection<TopicPartitionInitialOffset> partitionsAtBeginning = new HashSet<>();
    for (PartitionInfo partition : partitions) {
      partitionsAtBeginning.add(
          new TopicPartitionInitialOffset(partition.topic(), partition.partition(),
              SeekPosition.BEGINNING));
    }
    return partitionsAtBeginning;
  }

  private List<PartitionInfo> queryPartitions() {
    List<PartitionInfo> partitions;
    try (Consumer<? super K, ? super V> tempConsumer = containerFactory.getConsumerFactory()
        .createConsumer()) {
      partitions = tempConsumer.partitionsFor(topic.getName());
    }
    return partitions;
  }

  public Map<TopicPartition, TopicPartitionOffsets> queryPartitionOffsets() {
    Map<TopicPartition, TopicPartitionOffsets> offsets = new HashMap<>();
    try (Consumer<? super K, ? super V> tempConsumer = containerFactory.getConsumerFactory()
        .createConsumer()) {
      List<PartitionInfo> partitions = tempConsumer.partitionsFor(topic.getName());
      List<TopicPartition> topicPartitions = partitions.stream()
          .map(partition -> new TopicPartition(partition.topic(), partition.partition())).collect(
              Collectors.toList());

      Map<TopicPartition, Long> beginningOffsets = tempConsumer.beginningOffsets(topicPartitions);
      beginningOffsets.forEach((topicPartition, beginningOffset) -> offsets.put(topicPartition,
          new TopicPartitionOffsets(topicPartition.topic(), topicPartition.partition(),
              beginningOffset, -1)));

      Map<TopicPartition, Long> endOffsets = tempConsumer.endOffsets(topicPartitions);

      // merge endOffset and beginningOffset maps to into offsets map that contains both
      endOffsets.forEach((topicPartition, endOffset) -> offsets.compute(topicPartition,
          (topicPartition1, existingOffsetObj) -> {
            if (existingOffsetObj == null) {
              existingOffsetObj = new TopicPartitionOffsets(topicPartition1.topic(),
                  topicPartition1.partition(), -1, -1);
            }

            return new TopicPartitionOffsets(topicPartition1.topic(), topicPartition1.partition(),
                existingOffsetObj.getBeginningOffset(), endOffset);
          }));
    }

    return offsets;
  }

  @Override
  public void start() throws Exception {
    log.info("[{}] State: STARTING; Properties:\n{}", topic.getName(), kafkaProperties.toString());

    container = containerFactory.createContainer(partitionsAtBeginning(queryPartitions()));

    container.setErrorHandler((thrownException, data) -> {
      log.error("[{}] Exception occurred while processing record; shutting down. Record: {}",
          topic.getName(), data, thrownException);
      stop();
    });

    container.getContainerProperties().setMessageListener(messageListener);

    container.start();
    log.info("[{}] State: RUNNING", topic.getName());
  }

  @Override
  public void stop() {
    log.info("[{}] State: STOPPING", topic.getName());
    container.stop();
    log.info("[{}] State: STOPPED", topic.getName());
  }
}
