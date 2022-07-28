package com.brokencircuits.downloader.kafka;

import com.brokencircuits.download.messages.DownloadType;
import com.brokencircuits.downloader.domain.AriaResponseStatus;
import com.brokencircuits.downloader.domain.download.DownloadResult;
import com.brokencircuits.downloader.download.DownloadController;
import com.brokencircuits.downloader.messages.DownloadRequestKey;
import com.brokencircuits.downloader.messages.DownloadRequestMsg;
import com.brokencircuits.downloader.messages.DownloadStatusKey;
import com.brokencircuits.downloader.messages.DownloadStatusMsg;
import com.brokencircuits.downloader.messages.DownloadStatusValue;
import com.brokencircuits.downloader.publish.DownloaderStatusApi;
import com.brokencircuits.kissad.kafka.ByteKey;
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
  private final Publisher<ByteKey<DownloadStatusKey>, DownloadStatusMsg> downloadStatusPublisher;
  private final DownloaderStatusApi downloaderStatusApi;

  @Value("${download.downloader-id}")
  private long downloaderId;

  @KafkaListener(topics = TopicUtil.TOPIC_DOWNLOAD_COMMAND)
  public void listen(ConsumerRecord<ByteKey<DownloadRequestKey>, DownloadRequestMsg> message,
                     Acknowledgment acknowledgment) {
    log.info("Received Message: {}", message);
    acknowledgment.acknowledge();

    new Thread(() -> {
      if (message.value().getKey().getDownloaderId() == downloaderId) {
        downloaderStatusApi.tellClusterStatus(false);
        boolean isMagnet = false;
        if (message.value().getKey().getDownloadType().equals(DownloadType.MAGNET)) {
          isMagnet = true;
        }
        try {
          downloadController
              .doDownload(message.value().getValue().getUri(),
                  message.value().getValue().getDestinationDir(),
                  message.value().getValue().getDestinationFileName(), isMagnet,
                  status -> {
                    downloadStatusPublisher
                        .send(downloadStatus(message.value().getValue().getDownloadId(), status));
                    String pcntComplete = String.format("%.2f", (100 * ((double) status.getResult().getCompletedLength() / status.getResult().getTotalLength())));
                    log.info("{} {}% complete", status.getResult().getFiles().get(0).getPath(), pcntComplete);
                  },
                  (downloadedFile, completeStatus) -> downloadStatusPublisher
                      .send(downloadStatus(message.value().getValue().getDownloadId(),
                          completeStatus)));

        } catch (IOException | InterruptedException e) {
          log.error("Error occurred during download:", e);
          downloadStatusPublisher
              .send(errorStatus(message.value().getValue().getDownloadId(), e.getMessage()));
        } finally {
          downloaderStatusApi.tellClusterStatus(true);
        }
      } else {
        log.info("Ignoring {}, as it was for a different downloader", message);
      }
    }).start();
  }

  private KeyValue<ByteKey<DownloadStatusKey>, DownloadStatusMsg> errorStatus(Uuid downloadId,
                                                                              String errorMsg) {
    DownloadStatusKey key = DownloadStatusKey.newBuilder().setDownloadId(downloadId).build();
    DownloadStatusValue value = DownloadStatusValue.newBuilder()
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
        .build();
    return new KeyValue<>(
        ByteKey.from(key),
        DownloadStatusMsg.newBuilder().setKey(key).setValue(value).build());
  }

  private KeyValue<ByteKey<DownloadStatusKey>, DownloadStatusMsg> downloadStatus(Uuid downloadId,
                                                                                 AriaResponseStatus status) {
    DownloadResult result = status.getResult();
    DownloadStatusKey key = DownloadStatusKey.newBuilder().setDownloadId(downloadId).build();
    DownloadStatusValue value = DownloadStatusValue.newBuilder()
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
        .build();
    return new KeyValue<>(
        ByteKey.from(key),
        DownloadStatusMsg.newBuilder().setKey(key).setValue(value).build());
  }
}
