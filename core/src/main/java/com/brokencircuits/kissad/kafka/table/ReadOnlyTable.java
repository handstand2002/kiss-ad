package com.brokencircuits.kissad.kafka.table;

import com.brokencircuits.kissad.kafka.KeyValue;
import java.util.function.Consumer;

public interface ReadOnlyTable<K, V> {

  V get(K key);

  void all(Consumer<KeyValue<K, V>> consumer);
}
