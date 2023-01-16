package com.brokencircuits.kissad.util;

import lombok.Value;

@Value(staticConstructor = "of")
public class KeyValue<K, V> {

  K key;
  V value;
}
