package com.brokencircuits.downloader.config;

import com.brokencircuits.downloader.messages.DownloadRequestKey;
import com.brokencircuits.downloader.messages.DownloadRequestValue;
import com.brokencircuits.downloader.messages.DownloadStatusKey;
import com.brokencircuits.downloader.messages.DownloadStatusValue;
import com.brokencircuits.downloader.messages.DownloaderStatusKey;
import com.brokencircuits.downloader.messages.DownloaderStatusValue;
import com.brokencircuits.kissad.kafka.Topic;
import com.brokencircuits.kissad.topics.TopicUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class KafkaConfig {

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
  Topic<DownloaderStatusKey, DownloaderStatusValue> downloaderStatusTopic(
      @Value("${messaging.schema-registry-url}") String schemaRegistryUrl) {
    return TopicUtil.downloaderStatusTopic(schemaRegistryUrl);
  }

}
