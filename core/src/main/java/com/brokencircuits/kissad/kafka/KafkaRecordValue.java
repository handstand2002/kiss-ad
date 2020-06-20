package com.brokencircuits.kissad.kafka;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.kafka.common.header.Headers;

@Data
@AllArgsConstructor
public class KafkaRecordValue<V> {

  private V value;
  private long timestamp;
  private Headers headers;
  private String topic;
  private Integer partition;
  private long offset;
}
