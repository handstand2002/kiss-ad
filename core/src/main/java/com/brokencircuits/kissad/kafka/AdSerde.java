package com.brokencircuits.kissad.kafka;

import io.confluent.kafka.streams.serdes.avro.SpecificAvroDeserializer;
import java.util.Map;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;

public class AdSerde<T> implements Serde<T> {

  private static final Serializer innerSerializer = new ByteArraySerializer();
  private static final Deserializer innerDeserializer = new SpecificAvroDeserializer();
  private boolean isKey;

  @Override
  public void configure(Map<String, ?> configs, boolean isKey) {
    this.isKey = isKey;
    innerDeserializer.configure(configs, isKey);
  }

  @Override
  public void close() {
    innerDeserializer.close();
  }

  @Override
  public Serializer<T> serializer() {
    return innerSerializer;
  }

  @Override
  public Deserializer<T> deserializer() {
    return innerDeserializer;
  }
}
