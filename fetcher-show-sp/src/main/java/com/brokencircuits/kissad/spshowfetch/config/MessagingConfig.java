package com.brokencircuits.kissad.spshowfetch.config;

import com.brokencircuits.kissad.kafka.ByteKey;
import com.brokencircuits.kissad.kafka.Publisher;
import com.brokencircuits.kissad.kafka.Topic;
import com.brokencircuits.kissad.kafka.config.KafkaConfig;
import com.brokencircuits.kissad.messages.EpisodeMsg;
import com.brokencircuits.kissad.messages.EpisodeMsgKey;
import com.brokencircuits.kissad.messages.ShowMsg;
import com.brokencircuits.kissad.messages.ShowMsgKey;
import com.brokencircuits.kissad.topics.TopicUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(KafkaConfig.class)
public class MessagingConfig {

  @Bean
  Topic<ByteKey<ShowMsgKey>, ShowMsg> showQueueTopic(
      @Value("${messaging.schema-registry-url}") String schemaRegistryUrl) {
    return TopicUtil.showQueueTopic(schemaRegistryUrl);
  }

  @Bean
  Topic<ByteKey<EpisodeMsgKey>, EpisodeMsg> episodeQueueTopic(
      @Value("${messaging.schema-registry-url}") String schemaRegistryUrl) {
    return TopicUtil.episodeQueueTopic(schemaRegistryUrl);
  }

  @Bean
  Publisher<ByteKey<EpisodeMsgKey>, EpisodeMsg> episodeMsgPublisher(
      KafkaConfig props,
      Topic<ByteKey<EpisodeMsgKey>, EpisodeMsg> episodeQueueTopic) {
    return new Publisher<>(props.producerProps(), episodeQueueTopic);
  }
}
