package com.brokencircuits.kissad.table;

import com.brokencircuits.kissad.util.KeyValue;
import java.util.function.Consumer;

public interface ReadOnlyTable<K, V> {

  V get(K key);

  void all(Consumer<KeyValue<K, V>> consumer);
}
