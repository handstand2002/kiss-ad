package com.brokencircuits.kissad.kafka;

import lombok.Value;

@Value(staticConstructor = "of")
public class KeyValue<K, V> {

  K key;
  V value;
}
