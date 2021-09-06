package com.brokencircuits.kissad.kafka;

import com.brokencircuits.kissad.kafka.config.KafkaConfig;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class KConnectionBuilder {

  private final KafkaConfig config;

//  public <K,V> TableBuilder<K,V> tableBuilder(Topic<K,V> underlyingTopic) {
//    return new TableBuilder<>(underlyingTopic);
//  }
}
