package com.brokencircuits.kissad.showapi.config;

import com.brokencircuits.kissad.kafka.ByteKey;
import com.brokencircuits.kissad.kafka.ClusterConnectionProps;
import com.brokencircuits.kissad.kafka.Publisher;
import com.brokencircuits.kissad.kafka.Topic;
import com.brokencircuits.kissad.messages.ShowMsgKey;
import com.brokencircuits.kissad.messages.ShowMsgValue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class PublisherConfig {

  @Bean
  Publisher<ByteKey<ShowMsgKey>, ShowMsgValue> showMessagePublisher(
      Topic<ByteKey<ShowMsgKey>, ShowMsgValue> showTopic, ClusterConnectionProps clusterProps) {

    return new Publisher<>(clusterProps.asProperties(), showTopic);
  }

}
