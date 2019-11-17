package com.brokencircuits.kissad.kafka;

import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerializer;
import java.util.Map;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.ExtendedSerializer;
import org.apache.kafka.common.serialization.Serializer;

public class AdSerializer<T extends SpecificRecord> implements ExtendedSerializer<T> {

  private final Serializer<T> inner = new SpecificAvroSerializer<>();
  private boolean isKey;

  @Override
  public byte[] serialize(String topic, Headers headers, T data) {

    return new byte[0];
  }

  @Override
  public void configure(Map<String, ?> configs, boolean isKey) {
    inner.configure(configs, isKey);
  }

  @Override
  public byte[] serialize(String topic, T data) {
    return new byte[0];
  }

  @Override
  public void close() {

  }
}
