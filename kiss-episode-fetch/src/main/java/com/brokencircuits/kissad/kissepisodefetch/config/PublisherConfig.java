package com.brokencircuits.kissad.kissepisodefetch.config;

import com.brokencircuits.kissad.kafka.ByteKey;
import com.brokencircuits.kissad.kafka.ClusterConnectionProps;
import com.brokencircuits.kissad.kafka.Publisher;
import com.brokencircuits.kissad.kafka.Topic;
import com.brokencircuits.kissad.messages.EpisodeMsgKey;
import com.brokencircuits.kissad.messages.KissEpisodeExternalSrcMsg;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PublisherConfig {

  @Bean
  Publisher<ByteKey<EpisodeMsgKey>, KissEpisodeExternalSrcMsg> externalSrcMsgPublisher(
      Topic<ByteKey<EpisodeMsgKey>, KissEpisodeExternalSrcMsg> kissExternalSourceTopic,
      ClusterConnectionProps clusterConnectionProps) {
    return new Publisher<>(clusterConnectionProps.asProperties(), kissExternalSourceTopic);
  }

}
