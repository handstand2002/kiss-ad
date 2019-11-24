package com.brokencircuits.downloader.publish;

import com.brokencircuits.downloader.messages.DownloaderStatusKey;
import com.brokencircuits.downloader.messages.DownloaderStatusMsg;
import com.brokencircuits.downloader.messages.DownloaderStatusValue;
import com.brokencircuits.kissad.kafka.ByteKey;
import com.brokencircuits.kissad.kafka.Publisher;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DownloaderStatusApi {

  @Value("${download.downloader-id}")
  long downloaderId;

  private final Publisher<ByteKey<DownloaderStatusKey>, DownloaderStatusMsg> downloaderStatusPublisher;

  public void tellClusterStatus(boolean available) {
    DownloaderStatusKey key = DownloaderStatusKey.newBuilder().setDownloaderId(downloaderId)
        .build();
    DownloaderStatusValue value = DownloaderStatusValue.newBuilder().setIsAvailable(available)
        .build();
    downloaderStatusPublisher.send(ByteKey.from(key),
        DownloaderStatusMsg.newBuilder().setKey(key).setValue(value).build());
  }
}
