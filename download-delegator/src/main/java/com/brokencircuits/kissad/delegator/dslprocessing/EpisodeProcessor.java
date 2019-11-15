package com.brokencircuits.kissad.delegator.dslprocessing;

import static com.brokencircuits.kissad.util.PathUtil.addTrailingSlashToPath;

import com.brokencircuits.kissad.download.DownloadApi;
import com.brokencircuits.kissad.download.domain.DownloadStatus;
import com.brokencircuits.kissad.download.domain.DownloadType;
import com.brokencircuits.kissad.kafka.Publisher;
import com.brokencircuits.kissad.kafka.StateStoreDetails;
import com.brokencircuits.kissad.messages.EpisodeLink;
import com.brokencircuits.kissad.messages.EpisodeMsgKey;
import com.brokencircuits.kissad.messages.EpisodeMsgValue;
import com.brokencircuits.kissad.messages.ShowMsgKey;
import com.brokencircuits.kissad.messages.ShowMsgValue;
import com.brokencircuits.kissad.util.Uuid;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.processor.Processor;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.processor.PunctuationType;
import org.apache.kafka.streams.state.KeyValueStore;

@Slf4j
@RequiredArgsConstructor
public class EpisodeProcessor implements Processor<EpisodeMsgKey, EpisodeMsgValue> {

  private static final Pattern SEASON_PATTERN = Pattern.compile("<SEASON_(\\d+)>");
  private static final Pattern EPISODE_PATTERN = Pattern.compile("<EPISODE_(\\d+)>");

  private static final Queue<KeyValue<EpisodeMsgKey, EpisodeMsgValue>> episodesToDownload = new LinkedBlockingQueue<>();
  private static final Set<DownloadStatus> activeDownloads = new HashSet<>();

  private final DownloadApi downloadApi;
  private final Publisher<EpisodeMsgKey, EpisodeMsgValue> episodeStorePublisher;
  private final StateStoreDetails<ShowMsgKey, ShowMsgValue> showStoreDetails;
  private final StateStoreDetails<EpisodeMsgKey, EpisodeMsgValue> episodeStoreDetails;
  private final long minQualityGoal;
  private final String downloadFolder;

  private KeyValueStore<ShowMsgKey, ShowMsgValue> showStore;
  private KeyValueStore<EpisodeMsgKey, EpisodeMsgValue> episodeStore;

  @Override
  public void init(ProcessorContext context) {
    episodeStore = episodeStoreDetails.getStore(context);
    showStore = showStoreDetails.getStore(context);

    context.schedule(10000, PunctuationType.WALL_CLOCK_TIME, l -> {
      activeDownloads.forEach(download -> {
        if (download.isFinished() || download.getErrorCode() != 0) {
          activeDownloads.remove(download);
        }
      });

      if (activeDownloads.isEmpty() && !episodesToDownload.isEmpty()) {
        KeyValue<EpisodeMsgKey, EpisodeMsgValue> entry = episodesToDownload.poll();
        submitDownload(entry.key, entry.value);
      }
    });
  }

  @Override
  public void process(EpisodeMsgKey key, EpisodeMsgValue value) {
    EpisodeMsgValue previousEpisodeDownload = episodeStore.get(key);
    if (previousEpisodeDownload != null && previousEpisodeDownload.getDownloadTime() != null) {
      log.info("Episode has already been downloaded, skipping: {}|{}", key, value);
      return;
    }

    if (activeDownloads.isEmpty()) {
      submitDownload(key, value);
    } else {
      episodesToDownload.add(new KeyValue<>(key, value));
    }
  }

  private void submitDownload(EpisodeMsgKey key, EpisodeMsgValue value) {
    ShowMsgValue showMsg = showStore.get(key.getShowId());

    String destinationDir = addTrailingSlashToPath(downloadFolder);
    String destinationFileName = "UNKNOWN_" + Instant.now().getEpochSecond();
    if (showMsg == null) {
      log.warn("Show with ID {} has no entry in GKT; Downloading to {} with filename {}",
          key.getShowId(),
          destinationDir, destinationFileName);
    } else {
      destinationDir += addTrailingSlashToPath(showMsg.getFolderName());
      destinationFileName = createFileName(showMsg.getEpisodeNamePattern(), showMsg.getSeason(),
          key.getEpisodeNumber());
    }

    EpisodeLink linkForBestQuality = selectLink(value);

    DownloadStatus downloadStatus = downloadApi.submitDownload(linkForBestQuality.getUrl(),
        DownloadType.valueOf(linkForBestQuality.getType().name()), destinationDir,
        destinationFileName, completedStatus -> {
          if (completedStatus.isFinished() && completedStatus.getErrorCode() == 0) {
            episodeStorePublisher
                .send(key, completedValue(completedStatus, value, linkForBestQuality));
          }
        });

    activeDownloads.add(downloadStatus);
  }

  private EpisodeMsgValue completedValue(DownloadStatus completedStatus,
      EpisodeMsgValue value, EpisodeLink linkForBestQuality) {
    return EpisodeMsgValue.newBuilder()
        .setDownloadTime(completedStatus.getEndTime())
        .setDownloadedQuality(linkForBestQuality.getQuality())
        .setLatestLinks(value.getLatestLinks())
        .setMessageId(Uuid.randomUUID())
        .build();
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

  private EpisodeLink selectLink(EpisodeMsgValue value) {
    List<EpisodeLink> latestLinks = value.getLatestLinks();
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

  @Override
  public void close() {

  }
}
