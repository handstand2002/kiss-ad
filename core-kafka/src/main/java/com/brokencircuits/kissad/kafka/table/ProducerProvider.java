package com.brokencircuits.kissad.kafka.table;

import java.util.Properties;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.Serializer;

public interface ProducerProvider {

  Producer<byte[], byte[]> get(Properties props);

  static ProducerProvider getDefault() {
    Serializer<byte[]> byteSerializer = new ByteArraySerializer();
    return props -> new KafkaProducer<>(props, byteSerializer, byteSerializer);
  }
}
