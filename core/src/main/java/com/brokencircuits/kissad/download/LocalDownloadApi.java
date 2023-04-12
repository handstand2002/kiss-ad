package com.brokencircuits.kissad.download;

import com.brokencircuits.kissad.download.domain.DownloadRequest;
import com.brokencircuits.kissad.download.domain.DownloadStatus;
import com.brokencircuits.kissad.download.domain.DownloadType;
import com.brokencircuits.kissad.download.domain.SimpleDownloadResult;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class LocalDownloadApi {

  private final Function<DownloadRequest, CompletableFuture<SimpleDownloadResult>> onDownloadRequest;

  public void submitDownload(String uri, DownloadType downloadType,
      String destinationDir, String destinationFileName,
      Consumer<SimpleDownloadResult> onCompletion) {
    log.info("Submitting download:\n\tURI: {}\n\tType: {}\n\tDestinationDir: {}\n\tFileName: {}",
        uri, downloadType, destinationDir, destinationFileName);

    DownloadStatus status = new DownloadStatus(UUID.randomUUID(), uri, destinationDir,
        destinationFileName, downloadType);

    DownloadRequest request = DownloadRequest.builder()
        .type(status.getDownloadType())
        .destinationDir(status.getDestinationDir())
        .destinationFileName(status.getDestinationFileName())
        .downloadId(status.getDownloadId())
        .uri(status.getUri())
        .build();
    CompletableFuture<SimpleDownloadResult> future = onDownloadRequest.apply(request);
    future.thenAccept(onCompletion);
  }
}
