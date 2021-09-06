package com.brokencircuits.kissad.kafka.table;

import java.util.Properties;
import org.apache.kafka.clients.consumer.Consumer;

public interface ConsumerProvider {

  <K, V> Consumer<K, V> get(Properties props);
}
