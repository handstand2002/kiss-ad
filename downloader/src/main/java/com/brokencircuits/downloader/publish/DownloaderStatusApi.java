package com.brokencircuits.downloader.publish;

import com.brokencircuits.downloader.messages.DownloaderStatusKey;
import com.brokencircuits.downloader.messages.DownloaderStatusValue;
import com.brokencircuits.kissad.kafka.Publisher;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DownloaderStatusApi {

  @Value("${download.downloader-id}")
  long downloaderId;

  private final Publisher<DownloaderStatusKey, DownloaderStatusValue> downloaderStatusPublisher;

  public void tellClusterStatus(boolean available) {
    downloaderStatusPublisher.send(DownloaderStatusKey.newBuilder()
            .setDownloaderId(downloaderId).build(),
        DownloaderStatusValue.newBuilder().setIsAvailable(available).build());
  }
}
