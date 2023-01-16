package com.brokencircuits.kissad.kafka.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;

public interface RecordProcessAction<K, V> {

  void apply(ConsumerRecord<K, V> record);
}
