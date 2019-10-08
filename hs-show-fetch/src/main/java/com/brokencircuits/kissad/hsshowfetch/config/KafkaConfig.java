package com.brokencircuits.kissad.hsshowfetch.config;

import com.brokencircuits.kissad.hsshowfetch.kafka.StreamProperties;
import com.brokencircuits.kissad.kafka.KafkaProperties;
import com.brokencircuits.kissad.kafka.Publisher;
import com.brokencircuits.kissad.kafka.Topic;
import com.brokencircuits.kissad.messages.EpisodeMsgKey;
import com.brokencircuits.kissad.messages.EpisodeMsgValue;
import com.brokencircuits.kissad.messages.ShowMsgKey;
import com.brokencircuits.kissad.messages.ShowMsgValue;
import com.brokencircuits.kissad.topics.TopicUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaConfig {

  @Bean
  KafkaProperties kafkaProperties(StreamProperties streamProperties) {
    KafkaProperties props = new KafkaProperties();
    streamProperties.getUpdatedStreamConfig().forEach(props::add);
    return props;
  }

  @Bean
  Topic<ShowMsgKey, ShowMsgValue> showQueueTopic(
      @Value("${messaging.schema-registry-url}") String schemaRegistryUrl) {
    return TopicUtil.showQueueTopic(schemaRegistryUrl);
  }

  @Bean
  Topic<EpisodeMsgKey, EpisodeMsgValue> episodeQueueTopic(
      @Value("${messaging.schema-registry-url}") String schemaRegistryUrl) {
    return TopicUtil.episodeQueueTopic(schemaRegistryUrl);
  }

  @Bean
  Publisher<EpisodeMsgKey, EpisodeMsgValue> episodeMsgPublisher(KafkaProperties props,
      Topic<EpisodeMsgKey, EpisodeMsgValue> episodeQueueTopic) {
    return new Publisher<>(props, episodeQueueTopic);
  }

}
