package com.brokencircuits.kissad.kafka;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;

@Slf4j
@RequiredArgsConstructor
public class KeyValueStore<K, V> implements ReadOnlyKeyValueStore<K, V> {

  @Getter
  final private String name;
  @Getter
  final private Topic<K, V> builtOnTopic;

  private ReadOnlyKeyValueStore<K, V> store;

  public void initialize(KafkaStreams streams) {
    store = streams.store(name, QueryableStoreTypes.keyValueStore());
    log.info("Initialized store '{}'", name);
  }

  private void checkStoreInitialized() {
    if (store == null) {
      throw new IllegalStateException("State store " + name + " is not initialized yet");
    }
  }

  @Override
  public V get(K k) {
    checkStoreInitialized();
    return store.get(k);
  }

  @Override
  public KeyValueIterator<K, V> range(K k, K k1) {
    checkStoreInitialized();
    return store.range(k, k1);
  }

  @Override
  public KeyValueIterator<K, V> all() {
    checkStoreInitialized();
    return store.all();
  }

  @Override
  public long approximateNumEntries() {
    checkStoreInitialized();
    return store.approximateNumEntries();
  }
}
