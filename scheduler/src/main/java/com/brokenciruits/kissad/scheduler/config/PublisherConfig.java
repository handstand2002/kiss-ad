package com.brokenciruits.kissad.scheduler.config;

import com.brokencircuits.kissad.kafka.ByteKey;
import com.brokencircuits.kissad.kafka.ClusterConnectionProps;
import com.brokencircuits.kissad.kafka.Publisher;
import com.brokencircuits.kissad.kafka.Topic;
import com.brokencircuits.kissad.messages.ShowMsg;
import com.brokencircuits.kissad.messages.ShowMsgKey;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PublisherConfig {

  @Bean
  Publisher<ByteKey<ShowMsgKey>, ShowMsg> showTriggerPublisher(
      Topic<ByteKey<ShowMsgKey>, ShowMsg> showQueueTopic, ClusterConnectionProps props) {
    return new Publisher<>(props.asProperties(), showQueueTopic);
  }
}
