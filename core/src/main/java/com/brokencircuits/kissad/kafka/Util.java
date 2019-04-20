package com.brokencircuits.kissad.kafka;

import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import java.util.Collections;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.common.serialization.Serde;

public class Util {

  public static <T extends SpecificRecord> Serde<T> createAvroSerde(String schemaRegistryUrl,
      boolean forKey) {
    final SpecificAvroSerde<T> serde = new SpecificAvroSerde<>();
    serde.configure(Collections
            .singletonMap(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl),
        forKey);
    return serde;
  }
}
