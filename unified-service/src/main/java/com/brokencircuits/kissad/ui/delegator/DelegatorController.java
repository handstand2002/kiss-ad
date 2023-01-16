package com.brokencircuits.kissad.ui.delegator;

import static com.brokencircuits.kissad.util.PathUtil.addTrailingSlashToPath;

import com.brokencircuits.downloader.messages.DownloadRequestMsg;
import com.brokencircuits.kissad.download.LocalDownloadApi;
import com.brokencircuits.kissad.download.domain.DownloadType;
import com.brokencircuits.kissad.download.domain.SimpleDownloadResult;
import com.brokencircuits.kissad.messages.EpisodeLink;
import com.brokencircuits.kissad.messages.EpisodeMsg;
import com.brokencircuits.kissad.messages.EpisodeMsgKey;
import com.brokencircuits.kissad.messages.EpisodeMsgValue;
import com.brokencircuits.kissad.messages.ShowMsg;
import com.brokencircuits.kissad.messages.ShowMsgKey;
import com.brokencircuits.kissad.table.ReadWriteTable;
import com.brokencircuits.kissad.util.ByteKey;
import com.brokencircuits.kissad.util.Uuid;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DelegatorController {

  private static final Pattern SEASON_PATTERN = Pattern.compile("<SEASON_(\\d+)>");
  private static final Pattern EPISODE_PATTERN = Pattern.compile("<EPISODE_(\\d+)>");

  private final LocalDownloadApi downloadApi;
  private final Function<DownloadRequestMsg, CompletableFuture<SimpleDownloadResult>> onDownloadRequest;
  @Value("${delegator.min-quality}")
  private long minQualityGoal;
  @Value("${delegator.download-folder}")
  private String downloadFolder;

  private final ReadWriteTable<ByteKey<ShowMsgKey>, ShowMsg> showTable;
  private final ReadWriteTable<ByteKey<EpisodeMsgKey>, EpisodeMsg> episodeTable;

  public void process(EpisodeMsg msg) throws ExecutionException, InterruptedException {
    ByteKey<EpisodeMsgKey> key = new ByteKey<>(msg.getKey());
    EpisodeMsg previousEpisodeDownload = episodeTable.get(key);
    boolean isDownloaded = Optional.ofNullable(previousEpisodeDownload)
        .map(EpisodeMsg::getValue)
        .map(EpisodeMsgValue::getDownloadTime).isPresent();
    log.debug("Episode previously downloaded entry: {} | {}", key, previousEpisodeDownload);

    if (isDownloaded) {
      log.info("Episode has already been downloaded, skipping: ShowId {}, Episode {}",
          msg.getKey().getShowId().getShowId(), msg.getKey().getEpisodeNumber());
      return;
    }

    Future<Void> future = submitDownload(key, msg);

    // wait for download to complete
    future.get();
  }

  private Future<Void> submitDownload(ByteKey<EpisodeMsgKey> key, EpisodeMsg msg) {
    ByteKey<ShowMsgKey> showKey = new ByteKey<>(msg.getKey().getShowId());
    ShowMsg showMsg = showTable.get(showKey);

    String destinationDir = addTrailingSlashToPath(downloadFolder);
    String destinationFileName;
    if (showMsg == null) {
      log.error("Show with ID {} has no entry in GKT; Aborting download of episode {}",
          msg.getKey().getShowId(), destinationDir);
      return CompletableFuture.completedFuture(null);
    } else {
      destinationDir += addTrailingSlashToPath(showMsg.getValue().getFolderName());
      destinationFileName = createFileName(showMsg.getValue().getEpisodeNamePattern(),
          showMsg.getValue().getSeason(), msg.getKey().getEpisodeNumber());
    }

    EpisodeLink linkForBestQuality = selectLink(msg);

    CompletableFuture<Void> future = new CompletableFuture<>();
    downloadApi.submitDownload(
        linkForBestQuality.getUrl(),
        DownloadType.valueOf(linkForBestQuality.getType().name()), destinationDir,
        destinationFileName, result -> {
          future.complete(null);

          if (result.getErrorCode() == 0) {
            log.info("Marking episode complete: {}", msg);
            episodeTable.put(key, completedValue(msg, linkForBestQuality));
          } else {
            log.error("Download failed with result {}", result);
          }
        });

    return future;
  }

  private EpisodeMsg completedValue(EpisodeMsg msg, EpisodeLink linkForBestQuality) {
    EpisodeMsgValue value = EpisodeMsgValue.newBuilder()
        .setDownloadTime(Instant.now())
        .setDownloadedQuality(linkForBestQuality.getQuality())
        .setLatestLinks(msg.getValue().getLatestLinks())
        .setMessageId(Uuid.randomUUID())
        .build();
    return EpisodeMsg.newBuilder().setKey(msg.getKey()).setValue(value).build();
  }

  private String createFileName(String episodeNamePattern, Integer seasonNum, Long episodeNum) {

    Matcher matcher = SEASON_PATTERN.matcher(episodeNamePattern);
    if (matcher.find()) {
      int numDigits = Integer.parseInt(matcher.group(1));
      StringBuilder episodeString = new StringBuilder(String.valueOf(seasonNum));
      while (episodeString.length() < numDigits) {
        episodeString.insert(0, "0");
      }
      episodeNamePattern = episodeNamePattern.replace(matcher.group(0), episodeString);
    }

    matcher = EPISODE_PATTERN.matcher(episodeNamePattern);
    if (matcher.find()) {
      int numDigits = Integer.parseInt(matcher.group(1));
      StringBuilder episodeString = new StringBuilder(String.valueOf(episodeNum));
      while (episodeString.length() < numDigits) {
        episodeString.insert(0, "0");
      }
      episodeNamePattern = episodeNamePattern.replace(matcher.group(0), episodeString);
    }

    return episodeNamePattern;
  }

  private EpisodeLink selectLink(EpisodeMsg value) {
    List<EpisodeLink> latestLinks = value.getValue().getLatestLinks();
    latestLinks.sort(Comparator.comparingInt(EpisodeLink::getQuality));
    EpisodeLink bestLink = null;
    for (EpisodeLink latestLink : latestLinks) {
      bestLink = latestLink;
      if (bestLink.getQuality() > minQualityGoal) {
        return bestLink;
      }
    }
    return bestLink;
  }

}
