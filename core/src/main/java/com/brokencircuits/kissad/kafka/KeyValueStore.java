package com.brokencircuits.kissad.kafka;

import avro.shaded.com.google.common.collect.Maps;
import java.util.Map;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;

@Slf4j
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

  public Map<K, V> asMap() {
    KeyValueIterator<K, V> iter = getStore().all();
    Map<K, V> outputMap = Maps.newHashMap();
    while (iter.hasNext()) {
      KeyValue<K, V> entry = iter.next();
      outputMap.put(entry.key, entry.value);
    }
    iter.close();
    return outputMap;
  }

  public void initialize(KafkaStreams streams) {
    log.info("Initializing store {}", name);
    store = streams.store(name, QueryableStoreTypes.keyValueStore());
  }
}
