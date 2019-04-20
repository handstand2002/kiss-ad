package com.brokencircuits.kissad.kafka;

import lombok.Value;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.processor.StateStore;

@Value
public class Topic<K, V> {

  final private String name;
  final private Serde<K> keySerde;
  final private Serde<V> valueSerde;

  public Consumed<K, V> consumed() {
    return Consumed.with(keySerde, valueSerde);
  }

  public Produced<K, V> produced() {
    return Produced.with(keySerde, valueSerde);
  }

  public Materialized<K, V, StateStore> materialized() {
    return Materialized.with(keySerde, valueSerde);
  }


}
