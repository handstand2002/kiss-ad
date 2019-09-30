package com.brokencircuits.downloader.download;

import com.brokencircuits.downloader.aria.AriaApi;
import com.brokencircuits.downloader.domain.AriaResponseStatus;
import com.brokencircuits.downloader.domain.AriaResponseUriSubmit;
import com.brokencircuits.downloader.domain.download.DownloadResult;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
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

  public void doDownload(String uri, boolean isMagnet) throws IOException, InterruptedException {

    AriaResponseUriSubmit response = ariaApi.submitUri("test", uri);
    log.info("Response from request: {}", response);

    String downloadGid = response.getGid();
    boolean complete = false;
    long lastPollCompletedLength = 0;
    Instant lastActivity = Instant.now();

    Thread.sleep(10000);
    while (!complete) {
      Thread.sleep(downloadStatusPollDuration.toMillis());
      AriaResponseStatus status = queryStatus(downloadGid);

      // update values used to make sure it doesn't sit doing nothing forever
      if (lastPollCompletedLength != status.getResult().getCompletedLength()) {
        lastActivity = Instant.now();
        lastPollCompletedLength = status.getResult().getCompletedLength();
      } else {
        if (lastActivity.plus(inactivityTimeout).isBefore(Instant.now())) {
          // timed out
          ariaApi.removeDownload(downloadGid);
          return;
        }
      }

      DownloadResult result = status.getResult();
      // if gid is updated, query the new one next time
      downloadGid = result.getGid();

      log.info("Status: {}", result);
      // TODO: update kafka with download progress

      if (result.getCompletedLength() == result.getTotalLength()) {
        complete = true;
      }
    }
    if (isMagnet) {
      ariaApi.removeDownload(downloadGid);
    }
    log.info("Completed download");
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
