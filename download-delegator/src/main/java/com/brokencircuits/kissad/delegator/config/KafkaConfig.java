package com.brokencircuits.kissad.delegator.config;

import com.brokencircuits.downloader.messages.DownloadRequestKey;
import com.brokencircuits.downloader.messages.DownloadRequestMsg;
import com.brokencircuits.downloader.messages.DownloadStatusKey;
import com.brokencircuits.downloader.messages.DownloadStatusMsg;
import com.brokencircuits.kissad.kafka.AdminInterface;
import com.brokencircuits.kissad.kafka.ByteKey;
import com.brokencircuits.kissad.kafka.ClusterConnectionProps;
import com.brokencircuits.kissad.kafka.Publisher;
import com.brokencircuits.kissad.kafka.StateStoreDetails;
import com.brokencircuits.kissad.kafka.Topic;
import com.brokencircuits.kissad.messages.EpisodeMsg;
import com.brokencircuits.kissad.messages.EpisodeMsgKey;
import com.brokencircuits.kissad.messages.ShowMsg;
import com.brokencircuits.kissad.messages.ShowMsgKey;
import com.brokencircuits.kissad.topics.TopicUtil;
import com.brokencircuits.messages.Command;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class KafkaConfig {

  private static final String NOT_DOWNLOADED_EPISODE_STORE_NAME = "not-downloaded-episodes";
  private static final String STORE_SHOW = "shows";
  private static final String STORE_EPISODE = "episodes";

  @Bean
  @ConfigurationProperties(prefix = "messaging")
  ClusterConnectionProps clusterConnectionProps() {
    return new ClusterConnectionProps();
  }

  @Bean
  StateStoreDetails<ByteKey<EpisodeMsgKey>, EpisodeMsg> notDownloadedStoreDetails(
      Topic<ByteKey<EpisodeMsgKey>, EpisodeMsg> episodeQueueTopic,
      @Value("${messaging.schema-registry-url}") String schemaRegistryUrl) {
    return new StateStoreDetails<>(NOT_DOWNLOADED_EPISODE_STORE_NAME,
        episodeQueueTopic.getKeySerde(), episodeQueueTopic.getValueSerde());
  }

  @Bean
  Topic<ByteKey<EpisodeMsgKey>, EpisodeMsg> episodeQueueTopic(
      @Value("${messaging.schema-registry-url}") String schemaRegistryUrl) {
    return TopicUtil.episodeQueueTopic(schemaRegistryUrl);
  }

  @Bean
  Topic<ByteKey<EpisodeMsgKey>, EpisodeMsg> episodeStoreTopic(
      @Value("${messaging.schema-registry-url}") String schemaRegistryUrl) {
    return TopicUtil.episodeStoreTopic(schemaRegistryUrl);
  }

  @Bean
  Topic<ByteKey<DownloadRequestKey>, DownloadRequestMsg> downloadRequestTopic(
      @Value("${messaging.schema-registry-url}") String schemaRegistryUrl) {
    return TopicUtil.downloadRequestTopic(schemaRegistryUrl);
  }

  @Bean
  Topic<ByteKey<DownloadStatusKey>, DownloadStatusMsg> downloadStatusTopic(
      @Value("${messaging.schema-registry-url}") String schemaRegistryUrl) {
    return TopicUtil.downloadStatusTopic(schemaRegistryUrl);
  }

  @Bean
  Topic<ByteKey<ShowMsgKey>, ShowMsg> showStoreTopic(
      @Value("${messaging.schema-registry-url}") String schemaRegistryUrl) {
    return TopicUtil.showStoreTopic(schemaRegistryUrl);
  }

  @Bean
  StateStoreDetails<ByteKey<ShowMsgKey>, ShowMsg> showStoreDetails(
      @Value("${messaging.schema-registry-url}") String schemaRegistryUrl,
      Topic<ByteKey<ShowMsgKey>, ShowMsg> showStoreTopic) {
    return new StateStoreDetails<>(STORE_SHOW, showStoreTopic.getKeySerde(),
        showStoreTopic.getValueSerde());
  }

  @Bean
  StateStoreDetails<ByteKey<EpisodeMsgKey>, EpisodeMsg> episodeStoreDetails(
      Topic<ByteKey<EpisodeMsgKey>, EpisodeMsg> episodeStoreTopic,
      @Value("${messaging.schema-registry-url}") String schemaRegistryUrl) {
    return new StateStoreDetails<>(STORE_EPISODE, episodeStoreTopic.getKeySerde(),
        episodeStoreTopic.getValueSerde());
  }

  @Bean
  Publisher<ByteKey<DownloadRequestKey>, DownloadRequestMsg> publisher(
      ClusterConnectionProps props,
      Topic<ByteKey<DownloadRequestKey>, DownloadRequestMsg> downloadRequestTopic) {
    return new Publisher<>(props.asProperties(), downloadRequestTopic);
  }

  @Bean
  AdminInterface adminInterface(@Value("${messaging.schema-registry-url}") String schemaRegistryUrl,
      ClusterConnectionProps props) throws Exception {
    AdminInterface adminInterface = new AdminInterface(schemaRegistryUrl, props);
    adminInterface.registerCommand(Command.SKIP_EPISODE_RANGE, command -> {
      log.info("doing nothing with {}", command);
//      EpisodeMsgKey key = EpisodeMsgKey.newBuilder()
//          .setRawTitle()
//          .build()
    });
    adminInterface.start();
    return adminInterface;
  }

}
