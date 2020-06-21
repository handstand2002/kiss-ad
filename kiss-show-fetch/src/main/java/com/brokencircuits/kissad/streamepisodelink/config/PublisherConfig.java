package com.brokencircuits.kissad.streamepisodelink.config;

import com.brokencircuits.kissad.kafka.ByteKey;
import com.brokencircuits.kissad.kafka.ClusterConnectionProps;
import com.brokencircuits.kissad.kafka.Publisher;
import com.brokencircuits.kissad.kafka.Topic;
import com.brokencircuits.kissad.messages.KissEpisodePageKey;
import com.brokencircuits.kissad.messages.KissEpisodePageMessage;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PublisherConfig {

  @Bean
  Publisher<ByteKey<KissEpisodePageKey>, KissEpisodePageMessage> kissEpisodePageMessagePublisher(
      Topic<ByteKey<KissEpisodePageKey>, KissEpisodePageMessage> kissEpisodePageQueueTopic,
      ClusterConnectionProps props) {
    return new Publisher<>(props.asProperties(), kissEpisodePageQueueTopic);
  }

}
