package com.brokencircuits.kissad.delegator.config;

import com.brokencircuits.downloader.messages.DownloadRequestKey;
import com.brokencircuits.downloader.messages.DownloadRequestMsg;
import com.brokencircuits.kissad.kafka.ByteKey;
import com.brokencircuits.kissad.kafka.Publisher;
import com.brokencircuits.kissad.kafka.Topic;
import com.brokencircuits.kissad.kafka.config.KafkaConfig;
import com.brokencircuits.kissad.messages.EpisodeMsg;
import com.brokencircuits.kissad.messages.EpisodeMsgKey;
import com.brokencircuits.kissad.messages.ShowMsg;
import com.brokencircuits.kissad.messages.ShowMsgKey;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PublisherConfig {

  @Bean
  Publisher<ByteKey<EpisodeMsgKey>, EpisodeMsg> episodeStorePublisher(
      KafkaConfig kafkaConfig,
      Topic<ByteKey<EpisodeMsgKey>, EpisodeMsg> episodeStoreTopic) {
    return new Publisher<>(kafkaConfig.producerProps(), episodeStoreTopic);
  }

  @Bean
  Publisher<ByteKey<DownloadRequestKey>, DownloadRequestMsg> publisher(
      KafkaConfig kafkaConfig,
      Topic<ByteKey<DownloadRequestKey>, DownloadRequestMsg> downloadRequestTopic) {
    return new Publisher<>(kafkaConfig.producerProps(), downloadRequestTopic);
  }

  @Bean
  Publisher<ByteKey<ShowMsgKey>, ShowMsg> showQueuePublisher(
      KafkaConfig kafkaConfig,
      Topic<ByteKey<ShowMsgKey>, ShowMsg> showQueueTopic) {
    return new Publisher<>(kafkaConfig.producerProps(), showQueueTopic);
  }


}
