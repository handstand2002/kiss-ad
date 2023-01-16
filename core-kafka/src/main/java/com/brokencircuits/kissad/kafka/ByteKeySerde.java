package com.brokencircuits.kissad.kafka;

import com.brokencircuits.kissad.util.ByteKey;
import java.util.Map;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;

public class ByteKeySerde<T extends SpecificRecordBase> implements Serde<ByteKey<T>> {

  private final Serializer<ByteKey<T>> serializer = new ByteKeySerializer<>();
  private final Deserializer<ByteKey<T>> deserializer = new ByteKeyDeserializer<>();

  @Override
  public void configure(Map<String, ?> configs, boolean isKey) {
    serializer.configure(configs, isKey);
    deserializer.configure(configs, isKey);
  }

  @Override
  public void close() {
    serializer.close();
    deserializer.close();
  }

  @Override
  public Serializer<ByteKey<T>> serializer() {
    return serializer;
  }

  @Override
  public Deserializer<ByteKey<T>> deserializer() {
    return deserializer;
  }
}
