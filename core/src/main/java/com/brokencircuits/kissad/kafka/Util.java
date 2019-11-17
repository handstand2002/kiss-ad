package com.brokencircuits.kissad.kafka;

import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import java.util.Collections;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.utils.Time;
import org.apache.kafka.streams.state.Stores;
import org.apache.kafka.streams.state.internals.KeyValueStoreBuilder;

public class Util {

  public static <T extends SpecificRecord> Serde<T> createAvroSerde(String schemaRegistryUrl,
      boolean forKey) {
    final Serde<T> serde = new SpecificAvroSerde<>();
    serde.configure(Collections
            .singletonMap(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl),
        forKey);
    return serde;
  }

  public static <K, V> KeyValueStoreBuilder<K, V> keyValueStoreBuilder(String storeName,
      SerdePair<K, V> serdePair) {
    return new KeyValueStoreBuilder<>(Stores.persistentKeyValueStore(storeName),
        serdePair.getKeySerde(), serdePair.getValueSerde(), Time.SYSTEM);
  }

  public static <K, V> KeyValueStoreBuilder<K, V> keyValueStoreBuilder(
      StateStoreDetails<K, V> details) {
    return keyValueStoreBuilder(details.getStoreName(),
        new SerdePair<>(details.getKeySerde(), details.getValueSerde()));
  }

}
