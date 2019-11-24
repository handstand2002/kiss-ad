package com.brokencircuits.kissad.download;

import com.brokencircuits.download.messages.DownloadType;
import com.brokencircuits.downloader.messages.DownloadRequestKey;
import com.brokencircuits.downloader.messages.DownloadRequestMsg;
import com.brokencircuits.downloader.messages.DownloadRequestValue;
import com.brokencircuits.downloader.messages.DownloadStatusValue;
import com.brokencircuits.kissad.download.domain.DownloadStatus;
import com.brokencircuits.kissad.download.domain.DownloadStatusWrapper;
import com.brokencircuits.kissad.kafka.ByteKey;
import com.brokencircuits.kissad.kafka.Publisher;
import com.brokencircuits.kissad.util.Uuid;
import java.time.Duration;
import java.time.Instant;
import java.util.function.BiConsumer;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DownloadThread extends Thread {

  @Getter
  private final Uuid uuid;
  private final DownloadStatusWrapper statusWrapper;
  private final BiConsumer<DownloadStatus, DownloadThread> onCompletion;

  public DownloadThread(DownloadStatusWrapper statusWrapper, DownloadApi api,
      Publisher<ByteKey<DownloadRequestKey>, DownloadRequestMsg> requestPublisher,
      Duration startTimeout,
      BiConsumer<DownloadStatus, DownloadThread> onCompletion) {

    super(() -> {
      DownloadStatus status = statusWrapper.getStatus();

      DownloadRequestKey key = DownloadRequestKey.newBuilder()
          .setDownloadType(convertDownloadStatus(status.getDownloadType()))
          .setDownloaderId(api.getFreeDownloaderId())
          .build();

      DownloadRequestValue value = DownloadRequestValue.newBuilder()
          .setDestinationFileName(status.getDestinationFileName())
          .setUri(status.getUri())
          .setDownloadId(status.getDownloadId())
          .setDestinationDir(status.getDestinationDir())
          .build();

      statusWrapper.setDownloaderId(key.getDownloaderId());
      statusWrapper.setStartTime(Instant.now());
      requestPublisher.send(ByteKey.from(key),
          DownloadRequestMsg.newBuilder().setKey(key).setValue(value).build());

      Instant timeoutAtInstant = Instant.now().plus(startTimeout);
      try {

        while (!status.isFinished()
            && (status.isHasStarted() || Instant.now().isBefore(timeoutAtInstant))) {
          Thread.sleep(1000);
        }

        if (!status.isHasStarted()) {
          statusWrapper.setStatus("lost");
          statusWrapper.setErrorCode(1);
          statusWrapper.setErrorMessage("Download was sent to downloader but response never "
              + "arrived: " + status.toString());
        }

      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    });
    this.onCompletion = onCompletion;
    this.statusWrapper = statusWrapper;
    uuid = statusWrapper.getStatus().getDownloadId();
  }

  public void onStatusMessage(DownloadStatusValue message) {

    Long bytesDownloaded = message.getBytesDownloaded();
    Long bytesTotal = message.getBytesTotal();
    Instant updateTime = Instant.now();

    statusWrapper.setHasStarted(true);
    statusWrapper.setIsFinished(bytesDownloaded.equals(bytesTotal));
    statusWrapper.setResolvedDestinationPath(message.getDestinationPath());
    statusWrapper.setEndTime(statusWrapper.getStatus().isFinished() ? updateTime : null);
    statusWrapper.setLastUpdate(updateTime);
    statusWrapper.setBytesPerSecond(message.getBytesPerSecond());
    statusWrapper.setBytesDownloaded(message.getBytesDownloaded());
    statusWrapper.setBytesTotal(message.getBytesTotal());
    statusWrapper.setConnections(message.getConnections());
    statusWrapper.setErrorCode(message.getErrorCode());
    statusWrapper.setErrorMessage(message.getErrorMessage());
    statusWrapper.setNumPieces(message.getNumPieces());
    statusWrapper.setPieceLength(message.getPieceLength());
    statusWrapper.setNumSeeders(message.getNumSeeders());
    statusWrapper.setStatus(message.getStatus());

    if (statusWrapper.getStatus().isFinished()) {
      onCompletion.accept(statusWrapper.getStatus(), this);
    }

    log.info("Updating status object: {}%: {}",
        (double) Math.round((bytesDownloaded / (double) bytesTotal) * 10000) / 100,
        statusWrapper.getStatus());
  }

  private static DownloadType convertDownloadStatus(
      com.brokencircuits.kissad.download.domain.DownloadType inputType) {

    return com.brokencircuits.download.messages.DownloadType.valueOf(inputType.name());
  }
}
