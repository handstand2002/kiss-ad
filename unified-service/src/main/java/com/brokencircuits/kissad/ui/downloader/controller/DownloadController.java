package com.brokencircuits.kissad.ui.downloader.controller;

import com.brokencircuits.kissad.ui.downloader.aria.AriaApi;
import com.brokencircuits.kissad.ui.downloader.aria.AriaResponseStatus;
import com.brokencircuits.kissad.ui.downloader.aria.AriaResponseUriSubmit;
import com.brokencircuits.kissad.ui.downloader.domain.DownloadResult;
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DownloadController {

  private final AriaApi ariaApi;

  @Value("${download.aria.status-poll-interval}")
  private Duration downloadStatusPollDuration;
  @Value("${download.aria.inactivity-timeout}")
  private Duration inactivityTimeout;
  @Value("${download.download-folder-root}")
  private String downloadFolder;
  @Value("${download.overwrite-permission}")
  private boolean overwritePermission;
  @Value("${download.rename.retry-count}")
  private int renameRetryCount;
  @Value("${download.rename.retry-delay}")
  private Duration renameRetryDelay;
  @Value("${download.mock-download}")
  private boolean mockDownload;
  @Value("${download.mock-success}")
  private boolean mockSuccess;

  private final static Pattern FOLDER_PATH_PATTERN = Pattern.compile("[\\\\/]$");

  private String addTrailingSlashIfNeeded(String dir) {
    if (!FOLDER_PATH_PATTERN.matcher(dir).find()) {
      return dir + "/";
    }
    return dir;
  }

  public void doDownload(String uri, String destinationDir, String filename, boolean isMagnet,
      Consumer<AriaResponseStatus> onStatusPoll,
      BiConsumer<File, AriaResponseStatus> onDownloadComplete)
      throws IOException, InterruptedException {

    if (mockDownload) {
      log.info("Mocking download for {}", filename);
      log.info("Mock success: {}", mockSuccess);
      int errCode = mockSuccess ? 0 : 1;
      String errMsg = "mock err msg";
      onDownloadComplete.accept(new File("mock-file"), new AriaResponseStatus("00", "2.0",
          new DownloadResult(100, 1, "mockDir", 1, errCode, errMsg,
              Collections.emptyList(), Collections.emptyList(), "mockGid", "infoHash", 1, 1, 100,
              "Status", 100, 100, 1)));
      return;
    }

    AriaResponseUriSubmit response = ariaApi.submitUri("test", uri);
    log.info("Response from request: {}", response);

    String downloadGid = response.getGid();
    boolean complete = false;
    long lastPollCompletedLength = 0;
    Instant lastActivity = Instant.now();

    String downloadedToFilename = null;
    Thread.sleep(10000);
    AriaResponseStatus latestStatus = null;
    while (!complete) {
      Thread.sleep(downloadStatusPollDuration.toMillis());
      latestStatus = queryStatus(downloadGid);
      onStatusPoll.accept(latestStatus);

      // update values used to make sure it doesn't sit doing nothing forever
      if (lastPollCompletedLength != latestStatus.getResult().getCompletedLength()) {
        lastActivity = Instant.now();
        lastPollCompletedLength = latestStatus.getResult().getCompletedLength();
      } else {
        if (lastActivity.plus(inactivityTimeout).isBefore(Instant.now())) {
          // timed out
          ariaApi.removeDownload(downloadGid);
          return;
        }
      }

      DownloadResult result = latestStatus.getResult();
      // if gid is updated, query the new one next time
      downloadGid = result.getGid();

      if (result.getCompletedLength() == result.getTotalLength()) {
        complete = true;
        if (result.getFiles() != null && result.getFiles().size() > 0) {
          downloadedToFilename = result.getFiles().get(0).getPath();
          if (result.getFiles().size() > 1) {
            log.error("Too many files in collection, don't know how to handle this: {}",
                result.getFiles());
          }
        }
      }
    }
    if (isMagnet) {
      ariaApi.removeDownload(downloadGid);
    }
    if (downloadedToFilename != null) {
      new FileMoveThread(new File(downloadFolder, destinationDir), filename, onDownloadComplete,
          downloadedToFilename, latestStatus, overwritePermission, renameRetryCount,
          renameRetryDelay).start();
    }
  }

  /**
   * Query status of a download. If it was a torrent download, the initial gid will be for the
   * metadata, which will finish quickly, but is marked with "followedBy" and another GID. If there
   * is a "followedBy" in the response, this method will query again for the status of the following
   * download and return the status of it instead.
   */
  private AriaResponseStatus queryStatus(String downloadGid) throws IOException {
    AriaResponseStatus status = ariaApi.queryStatus(downloadGid);
    DownloadResult result = status.getResult();

    if (result.getFollowedBy() != null && result.getFollowedBy().size() == 1) {
      downloadGid = result.getFollowedBy().get(0);
      status = ariaApi.queryStatus(downloadGid);

    } else if (result.getFollowedBy() != null && result.getFollowedBy().size() > 1) {
      log.error("'Followed by' has more than 1 element, unsure what this means. "
          + "Status may not be accurate; {}", result);
    }

    return status;
  }

}
