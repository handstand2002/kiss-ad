package com.brokencircuits.kissad.delegator.config;

import com.brokencircuits.downloader.messages.DownloadRequestKey;
import com.brokencircuits.downloader.messages.DownloadRequestValue;
import com.brokencircuits.downloader.messages.DownloadStatusKey;
import com.brokencircuits.downloader.messages.DownloadStatusValue;
import com.brokencircuits.kissad.delegator.kafka.StreamProperties;
import com.brokencircuits.kissad.kafka.KafkaProperties;
import com.brokencircuits.kissad.kafka.Publisher;
import com.brokencircuits.kissad.kafka.StateStoreDetails;
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

  private static final String NOT_DOWNLOADED_EPISODE_STORE_NAME = "not-downloaded-episodes";
  private static final String STORE_SHOW = "shows";
  private static final String STORE_EPISODE = "episodes";

  @Bean
  KafkaProperties kafkaProperties(StreamProperties streamProperties) {
    KafkaProperties props = new KafkaProperties();
    streamProperties.getUpdatedStreamConfig().forEach(props::add);
    return props;
  }

  @Bean
  StateStoreDetails<EpisodeMsgKey, EpisodeMsgValue> notDownloadedStoreDetails(
      @Value("${messaging.schema-registry-url}") String schemaRegistryUrl) {
    return new StateStoreDetails<>(NOT_DOWNLOADED_EPISODE_STORE_NAME,
        TopicUtil.getKeySerde(schemaRegistryUrl), TopicUtil.getValueSerde(schemaRegistryUrl));
  }

  @Bean
  Topic<EpisodeMsgKey, EpisodeMsgValue> episodeQueueTopic(
      @Value("${messaging.schema-registry-url}") String schemaRegistryUrl) {
    return TopicUtil.episodeQueueTopic(schemaRegistryUrl);
  }

  @Bean
  Topic<EpisodeMsgKey, EpisodeMsgValue> episodeStoreTopic(
      @Value("${messaging.schema-registry-url}") String schemaRegistryUrl) {
    return TopicUtil.episodeStoreTopic(schemaRegistryUrl);
  }

  @Bean
  Topic<DownloadRequestKey, DownloadRequestValue> downloadRequestTopic(
      @Value("${messaging.schema-registry-url}") String schemaRegistryUrl) {
    return TopicUtil.downloadRequestTopic(schemaRegistryUrl);
  }

  @Bean
  Topic<DownloadStatusKey, DownloadStatusValue> downloadStatusTopic(
      @Value("${messaging.schema-registry-url}") String schemaRegistryUrl) {
    return TopicUtil.downloadStatusTopic(schemaRegistryUrl);
  }

  @Bean
  Topic<ShowMsgKey, ShowMsgValue> showStoreTopic(
      @Value("${messaging.schema-registry-url}") String schemaRegistryUrl) {
    return TopicUtil.showStoreTopic(schemaRegistryUrl);
  }

  @Bean
  StateStoreDetails<ShowMsgKey, ShowMsgValue> showStoreDetails(
      @Value("${messaging.schema-registry-url}") String schemaRegistryUrl) {
    return new StateStoreDetails<>(STORE_SHOW,
        TopicUtil.getKeySerde(schemaRegistryUrl), TopicUtil.getValueSerde(schemaRegistryUrl));
  }

  @Bean
  StateStoreDetails<EpisodeMsgKey, EpisodeMsgValue> episodeStoreDetails(
      @Value("${messaging.schema-registry-url}") String schemaRegistryUrl) {
    return new StateStoreDetails<>(STORE_EPISODE,
        TopicUtil.getKeySerde(schemaRegistryUrl), TopicUtil.getValueSerde(schemaRegistryUrl));
  }

  @Bean
  Publisher<DownloadRequestKey, DownloadRequestValue> publisher(KafkaProperties props,
      Topic<DownloadRequestKey, DownloadRequestValue> downloadRequestTopic) {
    return new Publisher<>(props, downloadRequestTopic);
  }

}
