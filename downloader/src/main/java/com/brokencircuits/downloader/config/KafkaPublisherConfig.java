package com.brokencircuits.downloader.config;

import com.brokencircuits.downloader.messages.DownloadStatusKey;
import com.brokencircuits.downloader.messages.DownloadStatusMsg;
import com.brokencircuits.downloader.messages.DownloaderStatusKey;
import com.brokencircuits.downloader.messages.DownloaderStatusMsg;
import com.brokencircuits.kissad.kafka.ByteKey;
import com.brokencircuits.kissad.kafka.ClusterConnectionProps;
import com.brokencircuits.kissad.kafka.Publisher;
import com.brokencircuits.kissad.kafka.Topic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaPublisherConfig {

  @Bean
  public Publisher<ByteKey<DownloadStatusKey>, DownloadStatusMsg> downloadStatusPublisher(
      ClusterConnectionProps props,
      Topic<ByteKey<DownloadStatusKey>, DownloadStatusMsg> downloadStatusTopic) {
    return new Publisher<>(props.asProperties(), downloadStatusTopic);
  }

  @Bean
  public Publisher<ByteKey<DownloaderStatusKey>, DownloaderStatusMsg> downloaderStatusPublisher(
      ClusterConnectionProps props,
      Topic<ByteKey<DownloaderStatusKey>, DownloaderStatusMsg> downloaderStatusTopic) {
    return new Publisher<>(props.asProperties(), downloaderStatusTopic);
  }

}
