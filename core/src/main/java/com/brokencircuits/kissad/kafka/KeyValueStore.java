package com.brokencircuits.kissad.kafka;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;

@RequiredArgsConstructor
public class KeyValueStore<K, V> {

  @Getter
  final private String name;
  private ReadOnlyKeyValueStore<K, V> store;

  public ReadOnlyKeyValueStore<K, V> getStore() {
    if (store == null) {
      throw new IllegalStateException("State store " + name + " is not initialized yet");
    }
    return store;
  }

  public void initialize(KafkaStreams streams) {
    store = streams.store(name, QueryableStoreTypes.keyValueStore());
  }
}
