package com.brokencircuits.kissad.showapi.config;

import com.brokencircuits.kissad.kafka.ClusterConnectionProps;
import com.brokencircuits.kissad.kafka.Publisher;
import com.brokencircuits.kissad.kafka.Topic;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class PublisherConfig {

  @Bean
  Publisher<String, String> showMessagePublisher(Topic<String, String> topic,
      ClusterConnectionProps clusterProps) {

    return new Publisher<>(clusterProps.asProperties(), topic);
  }

}
