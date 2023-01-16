package com.brokencircuits.kissad.table;

public interface ReadWriteTable<K, V> extends ReadOnlyTable<K, V> {

  void put(K key, V value);
}
