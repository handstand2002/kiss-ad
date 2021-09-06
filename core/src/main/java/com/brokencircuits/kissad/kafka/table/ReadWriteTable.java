package com.brokencircuits.kissad.kafka.table;

public interface ReadWriteTable<K, V> extends ReadOnlyTable<K, V> {

  V put(K key, V value);
}
