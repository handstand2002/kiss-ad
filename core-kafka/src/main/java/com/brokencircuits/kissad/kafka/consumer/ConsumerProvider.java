package com.brokencircuits.kissad.kafka.consumer;

import java.util.Properties;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.Deserializer;

public interface ConsumerProvider {

  Consumer<byte[], byte[]> get(Properties props);

  static ConsumerProvider getDefault() {
    Deserializer<byte[]> byteDeserializer = new ByteArrayDeserializer();
    return props -> new KafkaConsumer<>(props, byteDeserializer, byteDeserializer);
  }
}
