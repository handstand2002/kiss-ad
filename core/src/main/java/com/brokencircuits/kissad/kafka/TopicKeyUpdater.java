package com.brokencircuits.kissad.kafka;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.Serdes;
import org.springframework.kafka.support.TopicPartitionInitialOffset.SeekPosition;

@Slf4j
public class TopicKeyUpdater<K, V> {

  private final Topic<K, V> topic;
  private final ClusterConnectionProps clusterConnectionProps;

  private Producer<byte[], byte[]> producer;
  private AtomicLong processedRecords = new AtomicLong(0);
  private AtomicLong republishedRecords = new AtomicLong(0);

  private void initializeProducer() {
    Map<String, Object> kafkaProps = clusterConnectionProps.asObjectMap();
    kafkaProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);
    kafkaProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);

    producer = new KafkaProducer<>(kafkaProps);
  }

  public TopicKeyUpdater(Topic<K, V> topicObj, ClusterConnectionProps clusterConnectionProps)
      throws Exception {
    this.topic = topicObj;
    this.clusterConnectionProps = clusterConnectionProps;
    initializeProducer();

    Topic<byte[], byte[]> byteTopic = new Topic<>(topicObj.getName(), Serdes.ByteArray(),
        Serdes.ByteArray());

    AnonymousConsumer<byte[], byte[]> consumer = new AnonymousConsumer<>(byteTopic,
        clusterConnectionProps, SeekPosition.BEGINNING);

    Map<TopicPartition, TopicPartitionOffsets> uncaughtUpPartitions = new HashMap<>();
    Map<TopicPartition, TopicPartitionOffsets> partitionOffsets = consumer
        .queryPartitionOffsets();

    partitionOffsets.forEach((topicPartition, offsets) -> {
      if (offsets.getEndOffset() - offsets.getBeginningOffset() > 1) {
        uncaughtUpPartitions.put(topicPartition, offsets);
      }
    });

    Semaphore finishedProcessingSemaphore = new Semaphore(0);

    consumer.setMessageListener(record -> {

      processedRecords.incrementAndGet();
      if (record.value() != null) {
        String topic = record.topic();
        K deserializedKey = topicObj.getKeySerde().deserializer().deserialize(topic, record.key());
        byte[] reSerializedKey = topicObj.getKeySerde().serializer()
            .serialize(topic, deserializedKey);

        if (!Arrays.equals(reSerializedKey, record.key())) {
          ProducerRecord<byte[], byte[]> nullRecord = new ProducerRecord<>(topic, record.key(),
              null);
          producer.send(nullRecord);

          ProducerRecord<byte[], byte[]> newRecord = new ProducerRecord<>(topic, reSerializedKey,
              record.value());
          producer.send(newRecord);

          republishedRecords.incrementAndGet();
          log.info("Republished record under new schema ID with key: {}", deserializedKey);
        }
      }

      TopicPartition topicPartition = new TopicPartition(record.topic(), record.partition());
      uncaughtUpPartitions.computeIfPresent(topicPartition,
          (topicPartition1, topicPartitionOffsets) -> {
            if (record.offset() >= topicPartitionOffsets.getEndOffset() - 1) {
              // if record offset is higher than the end offset of partition when it started,
              // remove entry from "uncaughtUpPartitions"
              return null;
            }
            return topicPartitionOffsets;
          });

      if (uncaughtUpPartitions.isEmpty()) {
        log.info("Processed {} records, republished {} records", processedRecords.get(),
            republishedRecords.get());
        finishedProcessingSemaphore.release();
        consumer.stop();
      }
    });

    consumer.start();
    finishedProcessingSemaphore.acquire();
  }
}
