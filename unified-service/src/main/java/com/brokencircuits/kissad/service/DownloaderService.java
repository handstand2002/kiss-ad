package com.brokencircuits.kissad.service;

import com.brokencircuits.kissad.domain.downloader.DownloadResult;
import com.brokencircuits.kissad.domain.downloader.DownloadStatus;
import com.brokencircuits.kissad.domain.downloader.FileDetails;
import com.brokencircuits.kissad.downloader.aria.AriaApi;
import com.brokencircuits.kissad.downloader.aria.AriaResponseStatus;
import com.brokencircuits.kissad.downloader.aria.AriaResponseUriSubmit;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class DownloaderService {
  public static final String PATH_PREFIX_METADATA = "[METADATA]";
  private final AriaApi ariaApi;
  private final TaskScheduler taskScheduler;
  private final ApplicationEventPublisher applicationEventPublisher;

  @Value("${download.aria.status-poll-interval}")
  private Duration downloadStatusPollDuration;
  @Value("${download.aria.inactivity-timeout}")
  private Duration inactivityTimeout;
  @Value("${download.mock-download}")
  private boolean mockDownload;

  @Builder
  @lombok.Value
  public static class DownloadReference {
    CompletableFuture<DownloadStatus> onComplete = new CompletableFuture<>();
    Callable<DownloadStatus> eagerStatusPoller;
    Supplier<DownloadStatus> lazyStatusPoller;
  }

  public DownloadReference submitDownload(String uri, String downloadToDir)
      throws IOException, InterruptedException {

    if (mockDownload) {
      return setupMockDownload();
    }

    AtomicReference<DownloadStatus> latestStatus = new AtomicReference<>();

    AriaResponseUriSubmit response = ariaApi.submitUri(UUID.randomUUID().toString(), uri, downloadToDir);
    String downloadGid = response.getGid();
    Set<String> downloadGidsSeen = Collections.synchronizedSet(new HashSet<>());
    downloadGidsSeen.add(downloadGid);

    DownloadReference reference = DownloadReference.builder()
        .eagerStatusPoller(() -> queryStatus(downloadGid, downloadGidsSeen))
        .lazyStatusPoller(() -> {
          DownloadStatus lastStatus = latestStatus.get();
          if (lastStatus != null) {
            return lastStatus;
          }
          try {
            return queryStatus(downloadGid, downloadGidsSeen);
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        })
        .build();

    CompletableFuture<ScheduledFuture<?>> pollFutureRef = new CompletableFuture<>();
    reference.getOnComplete().whenComplete((ariaResponseStatus, throwable) -> {
      if (throwable == null) {
        log.info("Download {} completed", downloadGid);
      } else {
        log.info("Download {} terminated with exception", downloadGid, throwable);
      }

      try {
        removeRelatedDownloads(downloadGid);
      } catch (IOException e) {
        log.error("Unable to remove download from aria {} due to exception", downloadGid, e);
      }

      // cancel the scheduled poll task. whenComplete here is used to eliminate race conditions between the scheduled
      //  task being created and setting variable in pollFutureRef
      log.debug("Signaling that status poll should be canceled for {}", downloadGid);
      pollFutureRef.whenComplete((scheduledFuture, t1) -> {
        log.debug("Canceling status poll for {}", downloadGid);
        scheduledFuture.cancel(false);
      });
    });

    log.info("Response from aria for request: {}", response);

    AtomicLong lastPollCompletedLength = new AtomicLong(0);
    AtomicReference<Instant> lastActivity = new AtomicReference<>(Instant.now());

    ScheduledFuture<?> pollFuture = taskScheduler.scheduleAtFixedRate(() -> {
      try {
        latestStatus.set(queryStatus(downloadGid, downloadGidsSeen));
      } catch (IOException e) {
        log.error("Unable to query status of download {}", downloadGid, e);
        throw new RuntimeException(e);
      }
      DownloadStatus result = latestStatus.get();

      // update values used to make sure it doesn't sit doing nothing forever
      if (lastPollCompletedLength.get() != result.getCompletedLength()) {
        lastActivity.set(Instant.now());
        lastPollCompletedLength.set(result.getCompletedLength());
      } else {
        if (lastActivity.get().plus(inactivityTimeout).isBefore(Instant.now())) {
          try {
            reference.getOnComplete().completeExceptionally(new TimeoutException("Download timed out: " + downloadGid));
          } catch (Exception e) {
            log.error("Exception removing download {}", downloadGid, e);
          }
          return;
        }
      }

      String pcntComplete = String.format("%.2f", (100 * ((double) result.getCompletedLength() / result.getTotalLength())));
      log.info("{} {}% complete ({} connections)",
          result.getFiles().get(0).getPath(), pcntComplete,
          result.getConnections());

      if (result.getCompletedLength() == result.getTotalLength() && result.getTotalLength() > 100) {
        reference.getOnComplete().complete(result);
        log.info("Download complete with result {}", result);
      }
      applicationEventPublisher.publishEvent(result);
    }, downloadStatusPollDuration);

    log.info("Assigning the pollFutureRef for download {}", downloadGid);
    pollFutureRef.complete(pollFuture);

    return reference;
  }

  private DownloadReference setupMockDownload() {
    DownloadReference ref = DownloadReference.builder()
        .eagerStatusPoller(DownloaderService::mockResponse)
        .lazyStatusPoller(DownloaderService::mockResponse)
        .build();

    long startEpochSecond = Instant.now().getEpochSecond();
    AtomicReference<ScheduledFuture<?>> scheduledFutureRef = new AtomicReference<>(null);
    ScheduledFuture<?> scheduledFuture = taskScheduler.scheduleAtFixedRate(() -> {
      long currentEpochSecond = Instant.now().getEpochSecond();
      long pcntComplete = Math.min(100, currentEpochSecond - startEpochSecond);

      DownloadStatus result = DownloadStatus.builder()
          .rootGid("dummyGid")
          .title("Dummy Title")
          .completedLength(pcntComplete)
          .connections(1)
          .downloadSpeed(100)
          .numPieces(1)
          .numSeeders(0)
          .totalLength(100)
          .uploadLength(0)
          .uploadSpeed(0)
          .files(Collections.singletonList(DownloadStatus.FileStatus.builder()
              .completedLength(pcntComplete)
              .length(100)
              .path("test-path")
              .errorCode(0)
              .errorMessage(null)
              .status("active")
              .build()))
          .build();
      applicationEventPublisher.publishEvent(result);
      if (pcntComplete == 100) {
        scheduledFutureRef.get().cancel(false);
        ref.getOnComplete().complete(result);
      }
    }, downloadStatusPollDuration);
    scheduledFutureRef.set(scheduledFuture);
    return ref;
  }

  @NotNull
  private static DownloadStatus mockResponse() {
    return DownloadStatus.builder()
        .rootGid("dummy")
        .completedLength(100)
        .connections(1)
        .downloadSpeed(1)
        .files(Collections.emptyList())
        .numPieces(1)
        .numSeeders(0)
        .totalLength(100)
        .uploadLength(0)
        .uploadSpeed(0)
        .build();
  }

  private Map<String, AriaResponseStatus> getRelatedDownloads(String downloadGid) throws IOException {
    Map<String, AriaResponseStatus> results = new LinkedHashMap<>();
    AriaResponseStatus status = ariaApi.queryStatus(downloadGid);
    results.put(downloadGid, status);

    List<String> followedBy = status.getResult().getFollowedBy();
    if (followedBy != null) {
      for (String childGid : followedBy) {
        results.putAll(getRelatedDownloads(childGid));
      }
    }
    return results;
  }

  private void removeRelatedDownloads(String downloadGid) throws IOException {
    log.info("Removing all downloads related to {}", downloadGid);
    Map<String, AriaResponseStatus> relatedDownloads = getRelatedDownloads(downloadGid);
    log.info("Found {} downloads related to {}: {}", relatedDownloads.size(), downloadGid, relatedDownloads.keySet());
    for (Map.Entry<String, AriaResponseStatus> entry : relatedDownloads.entrySet()) {
      String gid = entry.getKey();

      try {
        ariaApi.removeDownload(gid);
      } catch (Exception e) {
        if (!isMetadataDownload(entry.getValue())) {
          log.info("Could not remove download {}", gid, e);
        }
      }
    }
  }

  /**
   * Query status of a download. If it was a torrent download, the initial gid will be for the
   * metadata, which will finish quickly, but is marked with "followedBy" and another GID. If there
   * is a "followedBy" in the response, this method will query again for the status of the following
   * download and return the status of it instead.
   */
  private DownloadStatus queryStatus(String downloadGid, Set<String> downloadGidsSeen) throws IOException {
    Map<String, AriaResponseStatus> relatedDownloads = getRelatedDownloads(downloadGid);
    for (String childGid : relatedDownloads.keySet()) {
      if (downloadGidsSeen.add(childGid)) {
        log.info("New child of download {}: {}", downloadGid, childGid);
      }
    }

    long completedLength = 0;
    long connections = 0;
    long downloadSpeed = 0;
    long numPieces = 0;
    long numSeeders = 0;
    long totalLength = 0;
    long uploadLength = 0;
    long uploadSpeed = 0;
    List<DownloadStatus.FileStatus> files = new LinkedList<>();

    boolean hasNonMetadataDownload = relatedDownloads.values().stream()
        .anyMatch(status -> !isMetadataDownload(status));

    String title = downloadGid;
    for (AriaResponseStatus status : relatedDownloads.values()) {
      DownloadResult result = status.getResult();
      if (isMetadataDownload(status)) {
        title = titleFromMetadata(status);
        if (hasNonMetadataDownload) {
          // skipping metadata download as long as there's a non metadata download
          continue;
        }
      }

      completedLength += result.getCompletedLength();
      connections = Math.max(result.getConnections(), connections);
      downloadSpeed += result.getDownloadSpeed();
      numPieces += result.getNumPieces();
      numSeeders = Math.max(result.getNumSeeders(), numSeeders);
      totalLength += result.getTotalLength();
      uploadLength += result.getUploadLength();
      uploadSpeed += result.getUploadSpeed();
      for (FileDetails file : result.getFiles()) {
        files.add(DownloadStatus.FileStatus.builder()
            .path(file.getPath())
            .length(file.getLength())
            .completedLength(file.getCompletedLength())
            .status(result.getStatus())
            .errorCode(result.getErrorCode())
            .errorMessage(result.getErrorMessage())
            .build());
      }
    }

    return DownloadStatus.builder()
        .rootGid(downloadGid)
        .title(title)
        .completedLength(completedLength)
        .connections(connections)
        .downloadSpeed(downloadSpeed)
        .files(files)
        .numPieces(numPieces)
        .numSeeders(numSeeders)
        .totalLength(totalLength)
        .uploadLength(uploadLength)
        .uploadSpeed(uploadSpeed)
        .build();
  }

  private String titleFromMetadata(AriaResponseStatus status) {
    List<FileDetails> files = status.getResult().getFiles();
    if (files.size() != 1 || !files.get(0).getPath().startsWith(PATH_PREFIX_METADATA)) {
      throw new IllegalArgumentException("Download is not a metadata download: " + status);
    }
    return files.get(0).getPath().substring(PATH_PREFIX_METADATA.length());
  }

  private boolean isMetadataDownload(AriaResponseStatus status) {
    return status.getResult().getFiles().size() == 1 && status.getResult().getFiles().get(0).getPath().startsWith(PATH_PREFIX_METADATA);
  }
}
