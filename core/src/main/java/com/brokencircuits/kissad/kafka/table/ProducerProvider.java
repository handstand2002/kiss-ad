package com.brokencircuits.kissad.kafka.table;

import java.util.Properties;
import org.apache.kafka.clients.producer.Producer;

public interface ProducerProvider {

  <K, V> Producer<K, V> get(Properties props);
}
