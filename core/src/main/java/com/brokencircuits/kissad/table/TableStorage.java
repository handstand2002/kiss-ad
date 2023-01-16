package com.brokencircuits.kissad.table;


import com.brokencircuits.kissad.util.KeyValue;
import java.util.function.Consumer;

public interface TableStorage<K, V> {

  void put(K key, V value);

  V get(K key);

  void all(Consumer<KeyValue<K, V>> consumer);
}
