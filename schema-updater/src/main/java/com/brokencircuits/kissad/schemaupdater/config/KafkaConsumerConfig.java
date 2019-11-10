package com.brokencircuits.kissad.schemaupdater.config;

import com.brokencircuits.kissad.kafka.ClusterConnectionProps;
import com.brokencircuits.kissad.kafka.Topic;
import com.brokencircuits.kissad.kafka.TopicKeyUpdater;
import com.brokencircuits.kissad.messages.ShowMsgKey;
import com.brokencircuits.kissad.messages.ShowMsgValue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class KafkaConsumerConfig {

  @Bean
  TopicKeyUpdater<ShowMsgKey, ShowMsgValue> showKeyUpdater(
      ClusterConnectionProps clusterConnectionProps, Topic<ShowMsgKey, ShowMsgValue> showTopic)
      throws Exception {
    return new TopicKeyUpdater<>(showTopic, clusterConnectionProps);
  }

}
