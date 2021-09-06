package com.brokencircuits.kissad.kafka.table;

import com.brokencircuits.kissad.kafka.Topic;
import com.brokencircuits.kissad.kafka.config.KafkaConfig;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

@Accessors(chain = true)
@RequiredArgsConstructor
public class TableBuilder<K, V> {

  private Topic<K, V> underlyingTopic;
  @Setter
  private TableStorage<K, V> storage;
  private final ConsumerProvider consumerProvider;
  private final ProducerProvider producerProvider;
  private final KafkaConfig config;

  public KafkaLiveTable<K, V> liveTable(Topic<K, V> underlyingTopic) {
    requireStorage();
    this.underlyingTopic = underlyingTopic;

    Map<String, Collection<RecordProcessAction<?, ?>>> recordHandler = new HashMap<>();
    recordHandler.put(underlyingTopic.getName(), Collections.singleton(
        (RecordProcessAction<K, V>) record -> storage.put(record.key(), record.value())));

    GlobalConsumerThread consumerThread = new GlobalConsumerThread(
        String.format("%s-%s", KafkaLiveTable.class.getSimpleName(), underlyingTopic.getName()),
        consumerProvider, Collections.singleton(underlyingTopic), recordHandler, new Properties());

    return new KafkaLiveTable<>(consumerThread, underlyingTopic, storage);
  }

  public KafkaBackedTable<K, V> backedTable(Topic<K, V> underlyingTopic) {
    requireStorage();
    this.underlyingTopic = underlyingTopic;

    Map<String, Collection<RecordProcessAction<?, ?>>> recordHandler = new HashMap<>();
    recordHandler.put(underlyingTopic.getName(), Collections.singleton(
        (RecordProcessAction<K, V>) record -> storage.put(record.key(), record.value())));

    GlobalConsumerThread consumerThread = new GlobalConsumerThread(
        String.format("%s-%s", KafkaLiveTable.class.getSimpleName(), underlyingTopic.getName()),
        consumerProvider, Collections.singleton(underlyingTopic), recordHandler, new Properties());
    return new KafkaBackedTable<>(consumerThread, underlyingTopic, storage);
  }

  private void requireStorage() {
    Objects.requireNonNull(storage, String
        .format("%s must be defined in %s", TableStorage.class.getSimpleName(),
            TableBuilder.class.getSimpleName()));
  }

  //type of wrapper (auto-updating, auto-backup, none)
}
