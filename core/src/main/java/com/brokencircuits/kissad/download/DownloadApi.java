package com.brokencircuits.kissad.download;

import com.brokencircuits.downloader.messages.DownloadRequestKey;
import com.brokencircuits.downloader.messages.DownloadRequestMsg;
import com.brokencircuits.downloader.messages.DownloadStatusMsg;
import com.brokencircuits.kissad.download.domain.DownloadStatus;
import com.brokencircuits.kissad.download.domain.DownloadStatusWrapper;
import com.brokencircuits.kissad.download.domain.DownloadType;
import com.brokencircuits.kissad.kafka.ByteKey;
import com.brokencircuits.kissad.kafka.Publisher;
import com.brokencircuits.kissad.util.Uuid;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class DownloadApi {

  private final Publisher<ByteKey<DownloadRequestKey>, DownloadRequestMsg> requestPublisher;
  private Map<Uuid, DownloadThread> activeDownloadThreads = new HashMap<>();

  public DownloadStatus submitDownload(String uri, DownloadType downloadType,
      String destinationDir, String destinationFileName, Consumer<DownloadStatus> onCompletion) {
    log.info("Submitting download:\n\tURI: {}\n\tType: {}\n\tDestinationDir: {}\n\tFileName: {}",
        uri, downloadType, destinationDir, destinationFileName);

    Uuid downloadId = Uuid.randomUUID();

    DownloadStatus status = new DownloadStatus(downloadId, uri, destinationDir, destinationFileName,
        downloadType);
    DownloadStatusWrapper statusWrapper = new DownloadStatusWrapper(status);

    DownloadThread downloadThread = new DownloadThread(statusWrapper, this, requestPublisher,
        Duration.ofSeconds(30), (completeStatus, thread) -> {
      log.info("Completed download: {}", completeStatus);
      activeDownloadThreads.remove(thread.getUuid());
      onCompletion.accept(completeStatus);
    });
    downloadThread.start();
    activeDownloadThreads.put(downloadId, downloadThread);

    return status;
  }

  public long getFreeDownloaderId() {
    // TODO: implement this;
    return 1L;
  }

  public void onDownloadStatusMessage(DownloadStatusMsg msg) {

    if (activeDownloadThreads.containsKey(msg.getKey().getDownloadId())) {
      activeDownloadThreads.get(msg.getKey().getDownloadId()).onStatusMessage(msg.getValue());
    } else {
      log.info("Download status message for inactive download: {}", msg);
    }
  }
}
