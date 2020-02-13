package com.brokencircuits.kissad.kafka;

import java.util.Properties;
import java.util.concurrent.Future;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.streams.KeyValue;

@Slf4j
@EqualsAndHashCode
@ToString
public class Publisher<K, V> {

  private final Producer<K, V> producer;
  private final Topic<K, V> topic;

  public Publisher(Properties producerProperties, Topic<K, V> topic) {
    this.topic = topic;
    this.producer = new KafkaProducer<>(producerProperties, topic.getKeySerde().serializer(),
        topic.getValueSerde().serializer());
  }

  public Future<RecordMetadata> send(K key, V message) {
    log.info("Sending to topic {}: {} | {}", topic.getName(), key, message);
    return producer.send(new ProducerRecord<>(topic.getName(), key, message));
  }

  public Future<RecordMetadata> send(K key, V value, String toTopic) {
    log.info("Sending to topic {}: {} | {}", toTopic, key, value);
    return producer.send(new ProducerRecord<>(toTopic, key, value));
  }

  public Future<RecordMetadata> send(KeyValue<K, V> pair) {
    return send(pair.key, pair.value);
  }

}
