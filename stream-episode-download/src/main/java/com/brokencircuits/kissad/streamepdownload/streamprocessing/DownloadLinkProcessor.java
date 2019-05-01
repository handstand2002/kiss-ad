package com.brokencircuits.kissad.streamepdownload.streamprocessing;

import com.brokencircuits.kissad.kafka.Publisher;
import com.brokencircuits.kissad.messages.DownloadAvailability;
import com.brokencircuits.kissad.messages.DownloadLink;
import com.brokencircuits.kissad.messages.DownloadedEpisodeKey;
import com.brokencircuits.kissad.messages.DownloadedEpisodeMessage;
import com.brokencircuits.kissad.messages.ExternalDownloadLinkKey;
import com.brokencircuits.kissad.messages.ExternalDownloadLinkMessage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.streams.processor.Processor;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DownloadLinkProcessor implements
    Processor<ExternalDownloadLinkKey, ExternalDownloadLinkMessage> {

  final private Publisher<String, DownloadAvailability> availabilityPublisher;
  final private Publisher<DownloadedEpisodeKey, DownloadedEpisodeMessage> downloadedEpisodePublisher;
  final private static Pattern dirHasTrailingSlashPattern = Pattern.compile("([\\\\/])$");

  @Value("${messaging.application-id}")
  private String applicationId;
  @Value("${download.destination}")
  private String destinationFolder;
  @Value("${download.attempts}")
  private int downloadAttempts;

  @Override
  public void init(ProcessorContext processorContext) {

    Matcher matcher = dirHasTrailingSlashPattern.matcher(destinationFolder);
    if (!matcher.find()) {
      destinationFolder += "/";
    }

    File destFolder = new File(destinationFolder);
    if (destFolder.exists() && destFolder.isFile()) {
      throw new IllegalStateException("Configured destination folder is actually a file");
    } else if (!destFolder.exists()) {
      if (!destFolder.mkdir()) {
        throw new IllegalStateException("Unable to create destination dir " + destFolder);
      }
    }
  }

  @Override
  public void process(ExternalDownloadLinkKey key, ExternalDownloadLinkMessage msg) {
    log.info("Processing {} | {}", key, msg);

    if (msg.getRetrieveTime().isBefore(DateTime.now().minusMinutes(5))) {
      log.info("Discarding link as expired");
      return;
    }

    int moreAttempts = downloadAttempts;
    availabilityPublisher
        .send(applicationId, DownloadAvailability.newBuilder().setAvailableCapacity(0).build());

    List<DownloadLink> orderedLinks = msg.getLinks();
    // sort by resolution, highest first
    orderedLinks.sort((o1, o2) -> o2.getResolution().compareTo(o1.getResolution()));

    log.info("Links: {}", orderedLinks);
    DownloadLink downloadLink = orderedLinks.get(0);

    String downloadUrl = downloadLink.getUrl();
    String destFolder = destinationFolder + key.getShowName() + "/";
    File destFolderFile = new File(destFolder);
    if (!destFolderFile.exists()) {
      if (!destFolderFile.mkdir()) {
        throw new IllegalStateException("Cannot create destination folder: " + destFolderFile);
      }
    }

    String destFile;
    if (msg.getSeasonNumber() != null && msg.getEpisodeNumber() != null) {
      destFile = destFolder + String
          .format("S%02dE%03d.mp4", msg.getSeasonNumber(), msg.getEpisodeNumber());
    } else {
      destFile = destFolder + key.getEpisodeName() + ".mp4";
    }

    try {

      URL toDownload = new URL(downloadUrl);
      log.info("Downloading {} to {}", toDownload, destFile);
      while (moreAttempts > 0) {
        try {
          moreAttempts--;
          FileUtils.copyURLToFile(toDownload, new File(destFile));
          log.info("Finished downloading {}", destFile);

          // publish episode to list of "Finished" episodes, so it won't try to retrieve this one again
          Future<RecordMetadata> sendFuture = downloadedEpisodePublisher
              .send(DownloadedEpisodeKey.newBuilder()
                      .setEpisodeName(key.getEpisodeName())
                      .setEpisodeNumber(msg.getEpisodeNumber())
                      .setSeasonNumber(msg.getSeasonNumber())
                      .setSubOrDub(msg.getSubOrDub())
                      .setShowName(key.getShowName())
                      .build(),
                  DownloadedEpisodeMessage.newBuilder()
                      .setRetrieveTime(DateTime.now()).build());
          log.info("Initial Future: {}", sendFuture);
          new Thread(() -> {
            while (!sendFuture.isDone()) {
              log.info("Future: {}", sendFuture);
              trySleep(10000);
            }
          }).start();
          trySleep(5000);
          log.info("Trying to get future...");
          RecordMetadata recordMetadata = sendFuture.get();
          log.info("Record meta: {}", recordMetadata);

        } catch (Exception e) {
          log.info("Failed to download {}; trying {} more times", destFile, moreAttempts);
        }
      }

    } catch (IOException e) {
      e.printStackTrace();
    }
    log.info("Finished processing {}", key);
    availabilityPublisher
        .send(applicationId, DownloadAvailability.newBuilder().setAvailableCapacity(1).build());
  }

  private void trySleep(long ms) {
    try {
      Thread.sleep(ms);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void close() {

  }
}
