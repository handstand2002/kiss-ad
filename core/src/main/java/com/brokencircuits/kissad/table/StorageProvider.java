package com.brokencircuits.kissad.table;

import com.brokencircuits.kissad.util.KeyValue;
import org.apache.kafka.common.serialization.Serde;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public interface StorageProvider {

  static StorageProvider hashMapBased() {
    return new StorageProvider() {
      @Override
      public <K, V> TableStorage<K, V> get(String name, Serde<K> keySerde, Serde<V> valueSerde) {
        final Map<K, V> inner = new ConcurrentHashMap<>();
        return new TableStorage<K, V>() {
          @Override
          public void put(K key, V value) {
            if (value == null) {
              inner.remove(key);
            } else {
              inner.put(key, value);
            }
          }

          @Override
          public V get(K key) {
            return inner.get(key);
          }

          @Override
          public void all(Consumer<KeyValue<K, V>> consumer) {
            inner.forEach((k, v) -> consumer.accept(KeyValue.of(k, v)));
          }
        };
      }
    };
  }

  <K, V> TableStorage<K, V> get(String name, Serde<K> keySerde, Serde<V> valueSerde);
}
