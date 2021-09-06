package com.brokencircuits.kissad.kafka;

import java.util.Map;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.common.serialization.Serializer;

public class ByteKeySerializer<T extends SpecificRecordBase> implements Serializer<ByteKey<T>> {

  @Override
  public void configure(Map<String, ?> configs, boolean isKey) {

  }

  @Override
  public byte[] serialize(String s, ByteKey<T> tByteKey) {
    return tByteKey.getBytes();
  }

  @Override
  public void close() {

  }
}
