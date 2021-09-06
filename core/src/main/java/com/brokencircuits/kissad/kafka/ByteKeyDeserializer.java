package com.brokencircuits.kissad.kafka;

import java.util.Map;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.common.serialization.Deserializer;

public class ByteKeyDeserializer<T extends SpecificRecordBase> implements Deserializer<ByteKey<T>> {

  @Override
  public void configure(Map<String, ?> configs, boolean isKey) {

  }

  @Override
  public ByteKey<T> deserialize(String s, byte[] bytes) {
    return new ByteKey<>(bytes);
  }

  @Override
  public void close() {

  }
}
