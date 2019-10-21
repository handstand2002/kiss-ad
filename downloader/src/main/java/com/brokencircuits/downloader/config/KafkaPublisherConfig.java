package com.brokencircuits.downloader.config;

import com.brokencircuits.downloader.messages.DownloadStatusKey;
import com.brokencircuits.downloader.messages.DownloadStatusValue;
import com.brokencircuits.downloader.messages.DownloaderStatusKey;
import com.brokencircuits.downloader.messages.DownloaderStatusValue;
import com.brokencircuits.kissad.kafka.ClusterConnectionProps;
import com.brokencircuits.kissad.kafka.Publisher;
import com.brokencircuits.kissad.kafka.Topic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaPublisherConfig {

  @Bean
  public Publisher<DownloadStatusKey, DownloadStatusValue> downloadStatusPublisher(
      ClusterConnectionProps props,
      Topic<DownloadStatusKey, DownloadStatusValue> downloadStatusTopic) {
    return new Publisher<>(props.asProperties(), downloadStatusTopic);
  }

  @Bean
  public Publisher<DownloaderStatusKey, DownloaderStatusValue> downloaderStatusPublisher(
      ClusterConnectionProps props,
      Topic<DownloaderStatusKey, DownloaderStatusValue> downloaderStatusTopic) {
    return new Publisher<>(props.asProperties(), downloaderStatusTopic);
  }

}
