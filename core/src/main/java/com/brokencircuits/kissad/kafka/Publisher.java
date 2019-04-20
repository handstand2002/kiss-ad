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
    log.info("Sending {} | {}", key, message);
    return producer.send(new ProducerRecord<>(topic.getName(), key, message));
  }

}
