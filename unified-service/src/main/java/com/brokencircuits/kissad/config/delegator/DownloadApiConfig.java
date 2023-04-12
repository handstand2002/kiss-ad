package com.brokencircuits.kissad.config.delegator;

import com.brokencircuits.kissad.download.LocalDownloadApi;
import com.brokencircuits.kissad.download.domain.DownloadRequest;
import com.brokencircuits.kissad.download.domain.SimpleDownloadResult;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DownloadApiConfig {

  @Bean
  LocalDownloadApi downloadApi(
      Function<DownloadRequest, CompletableFuture<SimpleDownloadResult>> onDownloadRequest) {
    return new LocalDownloadApi(onDownloadRequest);
  }
}
