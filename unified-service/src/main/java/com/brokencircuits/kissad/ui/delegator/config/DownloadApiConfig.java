package com.brokencircuits.kissad.ui.delegator.config;

import com.brokencircuits.downloader.messages.DownloadRequestMsg;
import com.brokencircuits.kissad.download.LocalDownloadApi;
import com.brokencircuits.kissad.download.domain.DownloadStatus;
import com.brokencircuits.kissad.download.domain.SimpleDownloadResult;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DownloadApiConfig {

  @Bean
  LocalDownloadApi downloadApi(
      Function<DownloadRequestMsg, CompletableFuture<SimpleDownloadResult>> onDownloadRequest) {
    return new LocalDownloadApi(onDownloadRequest);
  }
}
