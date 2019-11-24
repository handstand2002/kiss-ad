package com.brokencircuits.kissad.delegator.config;

import com.brokencircuits.kissad.kafka.ByteKey;
import com.brokencircuits.kissad.kafka.ClusterConnectionProps;
import com.brokencircuits.kissad.kafka.Publisher;
import com.brokencircuits.kissad.kafka.Topic;
import com.brokencircuits.kissad.messages.EpisodeMsg;
import com.brokencircuits.kissad.messages.EpisodeMsgKey;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PublisherConfig {

  @Bean
  Publisher<ByteKey<EpisodeMsgKey>, EpisodeMsg> episodeStorePublisher(
      ClusterConnectionProps kafkaProperties,
      Topic<ByteKey<EpisodeMsgKey>, EpisodeMsg> episodeStoreTopic) {
    return new Publisher<>(kafkaProperties.asProperties(), episodeStoreTopic);
  }
}
