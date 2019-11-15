package com.brokencircuits.downloader.kafka;

import com.brokencircuits.download.messages.DownloadType;
import com.brokencircuits.downloader.domain.AriaResponseStatus;
import com.brokencircuits.downloader.domain.download.DownloadResult;
import com.brokencircuits.downloader.download.DownloadController;
import com.brokencircuits.downloader.messages.DownloadRequestKey;
import com.brokencircuits.downloader.messages.DownloadRequestValue;
import com.brokencircuits.downloader.messages.DownloadStatusKey;
import com.brokencircuits.downloader.messages.DownloadStatusValue;
import com.brokencircuits.downloader.publish.DownloaderStatusApi;
import com.brokencircuits.kissad.kafka.Publisher;
import com.brokencircuits.kissad.topics.TopicUtil;
import com.brokencircuits.kissad.util.Uuid;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.streams.KeyValue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class Consumer {

  private final DownloadController downloadController;
  private final Publisher<DownloadStatusKey, DownloadStatusValue> downloadStatusPublisher;
  private final DownloaderStatusApi downloaderStatusApi;

  @Value("${download.downloader-id}")
  private long downloaderId;

  @KafkaListener(topics = TopicUtil.TOPIC_DOWNLOAD_COMMAND)
  public void listen(ConsumerRecord<DownloadRequestKey, DownloadRequestValue> message,
      Acknowledgment acknowledgment) {
    log.info("Received Message: {}", message);
    acknowledgment.acknowledge();

    new Thread(() -> {
      if (message.key().getDownloaderId() == downloaderId) {
        downloaderStatusApi.tellClusterStatus(false);
        boolean isMagnet = false;
        if (message.key().getDownloadType().equals(DownloadType.MAGNET)) {
          isMagnet = true;
        }
        try {
          downloadController
              .doDownload(message.value().getUri(), message.value().getDestinationDir(),
                  message.value().getDestinationFileName(), isMagnet,
                  status -> downloadStatusPublisher
                      .send(downloadStatus(message.value().getDownloadId(), status)),
                  (downloadedFile, completeStatus) -> downloadStatusPublisher
                      .send(downloadStatus(message.value().getDownloadId(), completeStatus)));

        } catch (IOException | InterruptedException e) {
          log.error("Error occurred during download:", e);
          downloadStatusPublisher
              .send(errorStatus(message.value().getDownloadId(), e.getMessage()));
        } finally {
          downloaderStatusApi.tellClusterStatus(true);
        }
      } else {
        log.info("Ignoring {}, as it was for a different downloader", message);
      }
    }).start();
  }

  private KeyValue<DownloadStatusKey, DownloadStatusValue> errorStatus(Uuid downloadId,
      String errorMsg) {
    return new KeyValue<>(
        DownloadStatusKey
            .newBuilder().setDownloadId(downloadId).build(),
        DownloadStatusValue.newBuilder()
            .setDestinationPath("")
            .setBytesPerSecond(0)
            .setBytesDownloaded(0)
            .setBytesTotal(0)
            .setConnections(0)
            .setErrorCode(0)
            .setErrorMessage(errorMsg)
            .setNumPieces(0)
            .setPieceLength(0)
            .setNumSeeders(0L)
            .setStatus("ERROR")
            .build());
  }

  private KeyValue<DownloadStatusKey, DownloadStatusValue> downloadStatus(Uuid downloadId,
      AriaResponseStatus status) {
    DownloadResult result = status.getResult();
    return new KeyValue<>(
        DownloadStatusKey
            .newBuilder().setDownloadId(downloadId).build(),
        DownloadStatusValue.newBuilder()
            .setDestinationPath(result.getFiles().get(0).getPath())
            .setBytesPerSecond(result.getDownloadSpeed())
            .setBytesDownloaded(result.getCompletedLength())
            .setBytesTotal(result.getTotalLength())
            .setConnections(result.getConnections())
            .setErrorCode(result.getErrorCode())
            .setErrorMessage(result.getErrorMessage())
            .setNumPieces(result.getNumPieces())
            .setPieceLength(result.getPieceLength())
            .setNumSeeders(result.getNumSeeders())
            .setStatus(result.getStatus())
            .build());
  }
}
