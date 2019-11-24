package com.brokencircuits.kissad.delegator.config;

import com.brokencircuits.downloader.messages.DownloadRequestKey;
import com.brokencircuits.downloader.messages.DownloadRequestMsg;
import com.brokencircuits.kissad.download.DownloadApi;
import com.brokencircuits.kissad.kafka.ByteKey;
import com.brokencircuits.kissad.kafka.Publisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DownloadApiConfig {

  @Bean
  DownloadApi downloadApi(Publisher<ByteKey<DownloadRequestKey>, DownloadRequestMsg> publisher) {
    return new DownloadApi(publisher);
  }
}
