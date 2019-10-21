package com.brokenciruits.kissad.scheduler.config;

import com.brokencircuits.kissad.kafka.ClusterConnectionProps;
import com.brokencircuits.kissad.kafka.Publisher;
import com.brokencircuits.kissad.kafka.Topic;
import com.brokencircuits.kissad.messages.ShowMsgKey;
import com.brokencircuits.kissad.messages.ShowMsgValue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PublisherConfig {

  @Bean
  Publisher<ShowMsgKey, ShowMsgValue> showTriggerPublisher(
      Topic<ShowMsgKey, ShowMsgValue> showQueueTopic, ClusterConnectionProps props) {
    return new Publisher<>(props.asProperties(), showQueueTopic);
  }
}
