package com.brokencircuits.kissad.download;

import com.brokencircuits.downloader.messages.DownloadRequestKey;
import com.brokencircuits.downloader.messages.DownloadRequestValue;
import com.brokencircuits.downloader.messages.DownloadStatusKey;
import com.brokencircuits.downloader.messages.DownloadStatusValue;
import com.brokencircuits.kissad.download.domain.DownloadStatus;
import com.brokencircuits.kissad.download.domain.DownloadStatusWrapper;
import com.brokencircuits.kissad.download.domain.DownloadType;
import com.brokencircuits.kissad.kafka.Publisher;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class DownloadApi {

  private final Publisher<DownloadRequestKey, DownloadRequestValue> requestPublisher;
  private Map<UUID, DownloadThread> activeDownloadThreads = new HashMap<>();

  public DownloadStatus submitDownload(String uri, DownloadType downloadType,
      String destinationDir, String destinationFileName) {

    UUID downloadId = UUID.randomUUID();

    DownloadStatus status = new DownloadStatus(downloadId, uri, destinationDir, destinationFileName,
        downloadType);
    DownloadStatusWrapper statusWrapper = new DownloadStatusWrapper(status);

    DownloadThread downloadThread = new DownloadThread(statusWrapper, this, requestPublisher,
        Duration.ofSeconds(30), (completeStatus, thread) -> {
      log.info("Completed download: {}", completeStatus);
      activeDownloadThreads.remove(thread.getUuid());
    });
    downloadThread.start();
    activeDownloadThreads.put(downloadId, downloadThread);

    return status;
  }

  public long getFreeDownloaderId() {
    // TODO: implement this;
    return 1L;
  }

  public void onDownloadStatusMessage(DownloadStatusKey key, DownloadStatusValue value) {
    UUID downloadId = UUID.fromString(key.getDownloadId());
    if (activeDownloadThreads.containsKey(downloadId)) {
      activeDownloadThreads.get(downloadId).onStatusMessage(value);
    } else {
      log.info("Download status message for inactive download: {} | {}", key, value);
    }
  }
}
