package com.brokencircuits.kissad.controller;

import static com.brokencircuits.kissad.util.PathUtil.addTrailingSlashToPath;

import com.brokencircuits.kissad.domain.EpisodeDto;
import com.brokencircuits.kissad.domain.EpisodeId;
import com.brokencircuits.kissad.domain.EpisodeLinkDto;
import com.brokencircuits.kissad.domain.RequestEpisode;
import com.brokencircuits.kissad.domain.RequestEpisodeResult;
import com.brokencircuits.kissad.domain.ShowDto;
import com.brokencircuits.kissad.download.LocalDownloadApi;
import com.brokencircuits.kissad.download.domain.DownloadType;
import com.brokencircuits.kissad.repository.EpisodeRepository;
import com.brokencircuits.kissad.repository.ShowRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
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
  @Value("${delegator.min-quality}")
  private long minQualityGoal;
  @Value("${delegator.download-folder}")
  private String downloadFolder;

  private final EpisodeRepository episodeRepository;
  private final ShowRepository showRepository;

  public RequestEpisodeResult process(RequestEpisode msg) throws ExecutionException, InterruptedException {
    Optional<EpisodeDto> previousEpisodeDownload = episodeRepository.findById(
        EpisodeId.builder().showId(msg.getShowId()).episodeNumber(msg.getEpisodeNumber()).build());

    boolean isDownloaded = previousEpisodeDownload
        .map(EpisodeDto::getDownloadTime)
        .isPresent();
    log.debug("Episode previously downloaded entry: {} | {}", msg, previousEpisodeDownload);

    if (isDownloaded) {
      log.info("Episode has already been downloaded, skipping: ShowId {}, Episode {}",
          msg.getShowId(), msg.getEpisodeNumber());
      return new RequestEpisodeResult(false, false);
    }

    Future<RequestEpisodeResult> future = submitDownload(msg);

    // wait for download to complete
    return future.get();
  }

  private Future<RequestEpisodeResult> submitDownload(RequestEpisode msg) {
    Optional<ShowDto> show = showRepository.findById(msg.getShowId());

    String destinationDir = addTrailingSlashToPath(downloadFolder);
    String destinationFileName;
    if (!show.isPresent()) {
      log.error("Show with ID {} has no entry in GKT; Aborting download of episode {}",
          msg.getShowId(), destinationDir);
      return CompletableFuture.completedFuture(null);
    } else {
      ShowDto showDto = show.get();
      destinationDir += addTrailingSlashToPath(showDto.getFolderName());
      destinationFileName = createFileName(showDto.getEpisodeNamePattern(),
          showDto.getSeason(), msg.getEpisodeNumber());
    }

    EpisodeLinkDto linkForBestQuality = selectLink(msg);

    CompletableFuture<RequestEpisodeResult> future = new CompletableFuture<>();
    downloadApi.submitDownload(
        linkForBestQuality.getUrl(),
        DownloadType.valueOf(linkForBestQuality.getType().name()), destinationDir,
        destinationFileName, result -> {

          if (result.getErrorCode() == 0) {
            log.info("Marking episode complete: {}", msg);
            episodeRepository.save(completedValue(msg, linkForBestQuality));
            future.complete(new RequestEpisodeResult(true, false));
          } else {
            log.error("Download failed with result {}", result);
            future.complete(new RequestEpisodeResult(false, true));
          }
        });

    return future;
  }

  private EpisodeDto completedValue(RequestEpisode msg, EpisodeLinkDto linkForBestQuality) {

    return EpisodeDto.builder()
        .showId(msg.getShowId())
        .episodeNumber(msg.getEpisodeNumber())
        .downloadTime(Instant.now())
        .downloadedQuality(linkForBestQuality.getQuality())
        .latestLinks(Collections.emptyList())
        .build();
  }

  private String createFileName(String episodeNamePattern, Integer seasonNum, int episodeNum) {

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

  private EpisodeLinkDto selectLink(RequestEpisode value) {

    List<EpisodeLinkDto> links = new ArrayList<>(value.getLinks());
    links.sort(Comparator.comparingInt(EpisodeLinkDto::getQuality));
    return links.stream()
        .filter(link -> link.getQuality() > minQualityGoal)
        .findFirst()
        .orElseGet(() -> {
          if (links.isEmpty()) {
            return null;
          }
          // get last link, since it would be highest quality
          return links.get(links.size() - 1);
        });
  }

}
