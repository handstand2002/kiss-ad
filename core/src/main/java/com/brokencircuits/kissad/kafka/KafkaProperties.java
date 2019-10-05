package com.brokencircuits.kissad.kafka;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class KafkaProperties extends Properties {

  public KafkaProperties add(Object key, Object value) {
    put(key, value);
    return this;
  }

  public Map<String, Object> asMap() {
    Map<String, Object> output = new HashMap<>();
    this.forEach((key, value) -> output.put(key.toString(), value));
    return output;
  }
}
