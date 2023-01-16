package com.brokencircuits.kissad.download;

import com.brokencircuits.downloader.messages.DownloadRequestKey;
import com.brokencircuits.downloader.messages.DownloadRequestMsg;
import com.brokencircuits.downloader.messages.DownloadRequestValue;
import com.brokencircuits.kissad.download.domain.DownloadStatus;
import com.brokencircuits.kissad.download.domain.DownloadType;
import com.brokencircuits.kissad.download.domain.SimpleDownloadResult;
import com.brokencircuits.kissad.util.Uuid;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class LocalDownloadApi {

  private final Function<DownloadRequestMsg, CompletableFuture<SimpleDownloadResult>> onDownloadRequest;

  public void submitDownload(String uri, DownloadType downloadType,
      String destinationDir, String destinationFileName,
      Consumer<SimpleDownloadResult> onCompletion) {
    log.info("Submitting download:\n\tURI: {}\n\tType: {}\n\tDestinationDir: {}\n\tFileName: {}",
        uri, downloadType, destinationDir, destinationFileName);

    Uuid downloadId = Uuid.randomUUID();

    DownloadStatus status = new DownloadStatus(downloadId, uri, destinationDir, destinationFileName,
        downloadType);

    DownloadRequestKey key = DownloadRequestKey.newBuilder()
        .setDownloadType(convertDownloadStatus(status.getDownloadType()))
        .setDownloaderId(getFreeDownloaderId())
        .build();

    DownloadRequestValue value = DownloadRequestValue.newBuilder()
        .setDestinationFileName(status.getDestinationFileName())
        .setUri(status.getUri())
        .setDownloadId(status.getDownloadId())
        .setDestinationDir(status.getDestinationDir())
        .build();

    DownloadRequestMsg requestMsg = DownloadRequestMsg.newBuilder()
        .setKey(key)
        .setValue(value)
        .build();

    CompletableFuture<SimpleDownloadResult> future = onDownloadRequest.apply(requestMsg);
    future.thenAccept(onCompletion);
  }

  public long getFreeDownloaderId() {
    // TODO: implement this;
    return 1L;
  }

  private static com.brokencircuits.download.messages.DownloadType convertDownloadStatus(
      DownloadType inputType) {

    return com.brokencircuits.download.messages.DownloadType.valueOf(inputType.name());
  }
}
