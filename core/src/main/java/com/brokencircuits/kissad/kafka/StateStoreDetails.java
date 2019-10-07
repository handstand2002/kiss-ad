package com.brokencircuits.kissad.kafka;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;

@RequiredArgsConstructor
@Getter
public class StateStoreDetails<K, V> {

  final private String storeName;
  final private Serde<K> keySerde;
  final private Serde<V> valueSerde;

  public KeyValueStore<K, V> getStore(ProcessorContext context) {
    return (KeyValueStore<K, V>) context.getStateStore(storeName);
  }

  public ReadOnlyKeyValueStore<K, V> getStore(KafkaStreams streams) {
    return streams.store(storeName, QueryableStoreTypes.keyValueStore());
  }
}
