package com.brokencircuits.kissad.download;

import com.brokencircuits.downloader.messages.DownloadRequestKey;
import com.brokencircuits.downloader.messages.DownloadRequestMsg;
import com.brokencircuits.downloader.messages.DownloadRequestValue;
import com.brokencircuits.downloader.messages.DownloadStatusValue;
import com.brokencircuits.kissad.download.domain.DownloadStatus;
import com.brokencircuits.kissad.download.domain.DownloadStatusWrapper;
import com.brokencircuits.kissad.util.Uuid;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Function;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DownloadThread extends Thread {

  @Getter
  private final Uuid uuid;
  private final DownloadStatusWrapper statusWrapper;
  private final BiConsumer<DownloadStatus, DownloadThread> onCompletion;
  private CompletableFuture<Boolean> downloadFuture;

  public DownloadThread(DownloadRequestMsg requestMsg, DownloadStatusWrapper statusWrapper,
                        Function<DownloadRequestMsg, CompletableFuture<Boolean>> onDownloadRequest,
                        Duration startTimeout,
                        BiConsumer<DownloadStatus, DownloadThread> onCompletion) {
    super(() -> {
      DownloadStatus status = statusWrapper.getStatus();

      DownloadRequestKey key = requestMsg.getKey();
      DownloadRequestValue value = requestMsg.getValue();

      statusWrapper.setDownloaderId(key.getDownloaderId());
      statusWrapper.setStartTime(Instant.now());
      CompletableFuture<Boolean> future = onDownloadRequest.apply(DownloadRequestMsg.newBuilder().setKey(key).setValue(value).build());
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

}
