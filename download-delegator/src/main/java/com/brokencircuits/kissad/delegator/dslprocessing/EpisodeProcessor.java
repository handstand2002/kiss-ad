package com.brokencircuits.kissad.delegator.dslprocessing;

import static com.brokencircuits.kissad.util.PathUtil.addTrailingSlashToPath;

import com.brokencircuits.kissad.download.DownloadApi;
import com.brokencircuits.kissad.download.domain.DownloadStatus;
import com.brokencircuits.kissad.download.domain.DownloadType;
import com.brokencircuits.kissad.kafka.ByteKey;
import com.brokencircuits.kissad.kafka.Publisher;
import com.brokencircuits.kissad.kafka.StateStoreDetails;
import com.brokencircuits.kissad.kafka.Topic;
import com.brokencircuits.kissad.messages.EpisodeLink;
import com.brokencircuits.kissad.messages.EpisodeMsg;
import com.brokencircuits.kissad.messages.EpisodeMsgKey;
import com.brokencircuits.kissad.messages.EpisodeMsgValue;
import com.brokencircuits.kissad.messages.ShowMsg;
import com.brokencircuits.kissad.messages.ShowMsgKey;
import com.brokencircuits.kissad.util.Uuid;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
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
public class EpisodeProcessor implements Processor<ByteKey<EpisodeMsgKey>, EpisodeMsg> {

  private static final Pattern SEASON_PATTERN = Pattern.compile("<SEASON_(\\d+)>");
  private static final Pattern EPISODE_PATTERN = Pattern.compile("<EPISODE_(\\d+)>");

  private static final Queue<KeyValue<ByteKey<EpisodeMsgKey>, EpisodeMsg>> episodesToDownload = new LinkedBlockingQueue<>();
  private static final Set<DownloadStatus> activeDownloads = new HashSet<>();

  private final DownloadApi downloadApi;
  private final Publisher<ByteKey<EpisodeMsgKey>, EpisodeMsg> episodeStorePublisher;
  private final Publisher<ByteKey<ShowMsgKey>, ShowMsg> showQueuePublisher;
  private final StateStoreDetails<ByteKey<ShowMsgKey>, ShowMsg> showStoreDetails;
  private final StateStoreDetails<ByteKey<EpisodeMsgKey>, EpisodeMsg> episodeStoreDetails;
  private final long minQualityGoal;
  private final String downloadFolder;

  private KeyValueStore<ByteKey<ShowMsgKey>, ShowMsg> showStore;
  private KeyValueStore<ByteKey<EpisodeMsgKey>, EpisodeMsg> episodeStore;

  @Override
  public void init(ProcessorContext context) {
    episodeStore = episodeStoreDetails.getStore(context);
    showStore = showStoreDetails.getStore(context);

    context.schedule(Duration.ofSeconds(10), PunctuationType.WALL_CLOCK_TIME, l -> {
      activeDownloads.forEach(download -> {
        if (download.isFinished() || download.getErrorCode() != 0) {
          activeDownloads.remove(download);
        }
      });

      if (activeDownloads.isEmpty() && !episodesToDownload.isEmpty()) {
        KeyValue<ByteKey<EpisodeMsgKey>, EpisodeMsg> entry = episodesToDownload.poll();
        submitDownload(entry.key, entry.value);
      }
    });
  }

  @Override
  public void process(ByteKey<EpisodeMsgKey> key, EpisodeMsg msg) {
    EpisodeMsg previousEpisodeDownload = episodeStore.get(key);
    boolean isDownloaded = Optional.ofNullable(previousEpisodeDownload)
        .map(EpisodeMsg::getValue)
        .map(EpisodeMsgValue::getDownloadTime).isPresent();
    log.debug("Episode previously downloaded entry: {} | {}", key, previousEpisodeDownload);

    if (isDownloaded) {
      log.info("Episode has already been downloaded, skipping: {}|{}", key, msg);
      return;
    }

    if (activeDownloads.isEmpty()) {
      submitDownload(key, msg);
    } else {
      episodesToDownload.add(new KeyValue<>(key, msg));
    }
  }

  private void submitDownload(ByteKey<EpisodeMsgKey> key, EpisodeMsg msg) {
    ByteKey<ShowMsgKey> showKey = new ByteKey<>(msg.getKey().getShowId());
    ShowMsg showMsg = showStore.get(showKey);

    String destinationDir = addTrailingSlashToPath(downloadFolder);
    String destinationFileName;
    if (showMsg == null) {
      log.error("Show with ID {} has no entry in GKT; Aborting download of episode {}", msg.getKey().getShowId(), destinationDir);
      return;
    } else {
      destinationDir += addTrailingSlashToPath(showMsg.getValue().getFolderName());
      destinationFileName = createFileName(showMsg.getValue().getEpisodeNamePattern(),
          showMsg.getValue().getSeason(), msg.getKey().getEpisodeNumber());
    }

    EpisodeLink linkForBestQuality = selectLink(msg);

    DownloadStatus downloadStatus = downloadApi.submitDownload(linkForBestQuality.getUrl(),
        DownloadType.valueOf(linkForBestQuality.getType().name()), destinationDir,
        destinationFileName, completedStatus -> {
          if (completedStatus.isFinished() && completedStatus.getErrorCode() == 0) {
            episodeStorePublisher
                .send(key, completedValue(completedStatus, msg, linkForBestQuality));
          } else if (completedStatus.getErrorCode() == 404) {
            log.info("Episode failed to download, requesting retry for show {}", msg.getKey().getRawTitle());
            showQueuePublisher.send(showKey, showMsg);
          }
        });

    activeDownloads.add(downloadStatus);
  }

  private EpisodeMsg completedValue(DownloadStatus completedStatus,
                                    EpisodeMsg msg, EpisodeLink linkForBestQuality) {
    EpisodeMsgValue value = EpisodeMsgValue.newBuilder()
        .setDownloadTime(completedStatus.getEndTime())
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

  @Override
  public void close() {

  }
}
