package com.brokencircuits.kissad.kafka.table;

import com.brokencircuits.kissad.table.ReadOnlyTable;
import com.brokencircuits.kissad.TableStorage;
import com.brokencircuits.kissad.util.KeyValue;
import com.brokencircuits.kissad.kafka.Topic;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import javax.annotation.PostConstruct;

import com.brokencircuits.kissad.kafka.consumer.GlobalConsumerThread;
import com.brokencircuits.kissad.kafka.consumer.TopicConsumerCatchupService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class KafkaLiveTable<K, V> implements ReadOnlyTable<K, V> {

  private final GlobalConsumerThread consumer;
  private final Topic<K, V> topic;
  private final TableStorage<byte[], byte[]> storage;


  private byte[] serializeKey(K key) {
    return Optional.ofNullable(key)
        .map(k -> topic.getKeySerde().serializer().serialize(topic.getName(), k))
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
  public void startConsumer() throws ExecutionException, InterruptedException {
    TopicConsumerCatchupService catchupService = new TopicConsumerCatchupService(consumer,
        topic.getName());

    consumer.start();

    catchupService.waitForCatchup();
  }

  @Override
  public V get(K key) {
    return deserializeValue(storage.get(serializeKey(key)));
  }

  @Override
  public void all(Consumer<KeyValue<K, V>> consumer) {
    storage.all(kv -> {
      K key = deserializeKey(kv.getKey());
      V value = deserializeValue(kv.getValue());
      consumer.accept(KeyValue.of(key, value));
    });
  }

}
