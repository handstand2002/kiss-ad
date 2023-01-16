package com.brokencircuits.kissad.kafka.table;

import com.brokencircuits.kissad.kafka.KeyValue;
import com.brokencircuits.kissad.kafka.Topic;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import javax.annotation.PostConstruct;

import com.brokencircuits.kissad.kafka.consumer.GlobalConsumerThread;
import com.brokencircuits.kissad.kafka.consumer.TopicConsumerCatchupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.utils.Bytes;

@Slf4j
@RequiredArgsConstructor
public class KafkaBackedTable<K, V> implements ReadWriteTable<K, V> {

  private final GlobalConsumerThread consumer;
  private final Producer<byte[], byte[]> producer;
  private final Topic<K, V> topic;
  private final TableStorage<Bytes, byte[]> storage;

  private byte[] serializeKey(K key) {
    return Optional.ofNullable(key)
        .map(k -> topic.getKeySerde().serializer().serialize(topic.getName(), k))
        .orElse(null);
  }

  private byte[] serializeValue(V value) {
    return Optional.ofNullable(value)
        .map(v -> topic.getValueSerde().serializer().serialize(topic.getName(), v))
        .orElse(null);
  }

  private K deserializeKey(byte[] key) {

    return Optional.ofNullable(key)
        .map(b -> topic.getKeySerde().deserializer().deserialize(topic.getName(), b))
        .orElse(null);
  }

  private V deserializeValue(byte[] bytes) {
    return Optional.ofNullable(bytes)
        .map(b -> topic.getValueSerde().deserializer().deserialize(topic.getName(), b))
        .orElse(null);
  }

  @PostConstruct
  public void initialize() throws ExecutionException, InterruptedException {
    TopicConsumerCatchupService catchupService = new TopicConsumerCatchupService(consumer,
        topic.getName());

    consumer.start();

    catchupService.waitForCatchup();
  }

  @Override
  public synchronized V get(K key) {
    return deserializeValue(storage.get(Bytes.wrap(serializeKey(key))));
  }

  @Override
  public void all(Consumer<KeyValue<K, V>> consumer) {
    storage.all(kv -> {
      K key = deserializeKey(kv.getKey().get());
      V value = deserializeValue(kv.getValue());
      consumer.accept(KeyValue.of(key, value));
    });
  }

  @Override
  public synchronized void put(K key, V value) {
    byte[] keyBytes = serializeKey(key);
    byte[] valueBytes = serializeValue(value);

    producer.send(new ProducerRecord<>(topic.getName(), keyBytes, valueBytes));
    storage.put(Bytes.wrap(keyBytes), valueBytes);
  }
}
