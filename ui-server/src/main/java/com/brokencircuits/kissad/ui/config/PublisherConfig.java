package com.brokencircuits.kissad.ui.config;

import com.brokencircuits.kissad.kafka.ByteKey;
import com.brokencircuits.kissad.kafka.Publisher;
import com.brokencircuits.kissad.kafka.Topic;
import com.brokencircuits.kissad.kafka.config.KafkaConfig;
import com.brokencircuits.kissad.messages.EpisodeMsg;
import com.brokencircuits.kissad.messages.EpisodeMsgKey;
import com.brokencircuits.kissad.messages.ShowMsg;
import com.brokencircuits.kissad.messages.ShowMsgKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class PublisherConfig {

  @Bean
  Publisher<ByteKey<ShowMsgKey>, ShowMsg> showMessagePublisher(
      Topic<ByteKey<ShowMsgKey>, ShowMsg> showTopic, KafkaConfig kafkaConfig) {

    return new Publisher<>(kafkaConfig.producerProps(), showTopic);
  }

  @Bean
  Publisher<ByteKey<EpisodeMsgKey>, EpisodeMsg> showEpisodePublisher(
      KafkaConfig kafkaConfig,
      Topic<ByteKey<EpisodeMsgKey>, EpisodeMsg> episodeStoreTopic) {

    return new Publisher<>(kafkaConfig.producerProps(), episodeStoreTopic);
  }

}