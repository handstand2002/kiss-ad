package com.brokencircuits.downloader.kafka;

import com.brokencircuits.download.messages.DownloadType;
import com.brokencircuits.downloader.download.DownloadController;
import com.brokencircuits.downloader.messages.DownloadRequestKey;
import com.brokencircuits.downloader.messages.DownloadRequestValue;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class Consumer {

  private final DownloadController downloadController;

  @Value("${download.downloader-id}")
  long downloaderId;

  @KafkaListener(topics = "${messaging.topics.download-instruction}")
  public void listen(ConsumerRecord<DownloadRequestKey, DownloadRequestValue> message,
      Acknowledgment acknowledgment) {
    log.info("Received Message: {}", message);
    if (message.key().getDownloaderId().equals(downloaderId)) {
      boolean isMagnet = false;
      if (message.key().getDownloadType().equals(DownloadType.MAGNET)) {
        isMagnet = true;
      }
      try {
        downloadController.doDownload(message.value().getUri(), isMagnet);
        // TODO: report download success
      } catch (IOException | InterruptedException e) {
        log.error("Error occurred during download:", e);
        // TODO: report download as failed
      }
    } else {
      log.info("Ignoring {}, as it was for a different downloader");
    }
    acknowledgment.acknowledge();
  }
}
