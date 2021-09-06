package com.brokencircuits.kissad.kafka.table;

import com.brokencircuits.kissad.kafka.KeyValue;
import java.util.function.Consumer;

public interface TableStorage<K, V> {

  V put(K key, V value);

  V get(K key);

  void all(Consumer<KeyValue<K, V>> consumer);
}
