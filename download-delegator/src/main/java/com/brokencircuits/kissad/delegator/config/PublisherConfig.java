package com.brokencircuits.kissad.delegator.config;

import com.brokencircuits.kissad.kafka.Publisher;
import com.brokencircuits.kissad.kafka.Topic;
import com.brokencircuits.kissad.messages.EpisodeMsgKey;
import com.brokencircuits.kissad.messages.EpisodeMsgValue;
import java.util.Properties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PublisherConfig {

  @Bean
  Publisher<EpisodeMsgKey, EpisodeMsgValue> episodeStorePublisher(Properties kafkaProperties,
      Topic<EpisodeMsgKey, EpisodeMsgValue> episodeStoreTopic) {
    return new Publisher<>(kafkaProperties, episodeStoreTopic);
  }
}
