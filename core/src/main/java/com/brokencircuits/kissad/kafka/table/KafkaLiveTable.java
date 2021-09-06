package com.brokencircuits.kissad.kafka.table;

import com.brokencircuits.kissad.kafka.KeyValue;
import com.brokencircuits.kissad.kafka.Topic;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class KafkaLiveTable<K, V> implements ReadOnlyTable<K, V> {

  private final GlobalConsumerThread consumer;
  private final Topic<K, V> topic;
  private final TableStorage<K, V> storage;

  @PostConstruct
  public void startConsumer() throws ExecutionException, InterruptedException {
    TopicConsumerCatchupService catchupService = new TopicConsumerCatchupService(consumer,
        topic.getName());

    consumer.start();

    catchupService.waitForCatchup();
  }

  @Override
  public V get(K key) {
    return storage.get(key);
  }

  @Override
  public void all(Consumer<KeyValue<K, V>> consumer) {
    storage.all(consumer);
  }
}
