package com.brokencircuits.kissad.kafka;

import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;

public class ByteKeySerde<T extends SpecificRecordBase> implements Serde<ByteKey<T>> {

  private final Serializer<ByteKey<T>> serializer = new ByteKeySerializer<>();
  private final Deserializer<ByteKey<T>> deserializer = new ByteKeyDeserializer<>();

  @Override
  public Serializer<ByteKey<T>> serializer() {
    return serializer;
  }

  @Override
  public Deserializer<ByteKey<T>> deserializer() {
    return deserializer;
  }
}
