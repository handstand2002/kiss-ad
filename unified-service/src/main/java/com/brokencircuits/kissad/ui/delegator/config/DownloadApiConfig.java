package com.brokencircuits.kissad.ui.delegator.config;

import com.brokencircuits.downloader.messages.DownloadRequestMsg;
import com.brokencircuits.kissad.download.LocalDownloadApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

@Configuration
public class DownloadApiConfig {

  @Bean
  LocalDownloadApi downloadApi(Function<DownloadRequestMsg, CompletableFuture<Boolean>> onDownloadRequest) {
    return new LocalDownloadApi(onDownloadRequest);
  }
}
