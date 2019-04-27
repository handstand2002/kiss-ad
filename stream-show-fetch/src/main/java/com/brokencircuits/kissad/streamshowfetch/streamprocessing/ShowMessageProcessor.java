package com.brokencircuits.kissad.streamshowfetch.streamprocessing;

import com.brokencircuits.kissad.kafka.Publisher;
import com.brokencircuits.kissad.kissweb.KissWebFetcher;
import com.brokencircuits.kissad.messages.DownloadedEpisodeKey;
import com.brokencircuits.kissad.messages.DownloadedEpisodeMessage;
import com.brokencircuits.kissad.messages.KissEpisodePageKey;
import com.brokencircuits.kissad.messages.KissEpisodePageMessage;
import com.brokencircuits.kissad.messages.KissShowMessage;
import com.brokencircuits.kissad.streamshowfetch.extractor.EpisodeMessageExtractor;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.processor.Processor;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ShowMessageProcessor implements Processor<Long, KissShowMessage> {

  final private KissWebFetcher webFetcher;
  final private Publisher<KissEpisodePageKey, KissEpisodePageMessage> episodeMessagePublisher;
  final private Publisher<DownloadedEpisodeKey, DownloadedEpisodeMessage> downloadedEpisodePublisher;
  final private EpisodeMessageExtractor extractor;

  @Value("${messaging.stores.downloaded-episode}")
  private String downloadedEpisodeStoreName;

  private ReadOnlyKeyValueStore<DownloadedEpisodeKey, DownloadedEpisodeMessage> downloadedEpisodeStore;
  final private static Pattern PARSE_RANGE_PATTERN = Pattern
      .compile("\\s*(\\d+)\\s*(-)?\\s*(\\d*)\\s*");

  @Override
  public void init(ProcessorContext context) {
    downloadedEpisodeStore = (ReadOnlyKeyValueStore<DownloadedEpisodeKey, DownloadedEpisodeMessage>) context
        .getStateStore(downloadedEpisodeStoreName);
  }

  @Override
  public void process(Long showId, KissShowMessage showMessage) {
    // on failure to fully process, throw into a "failed" topic

    log.info("Processing {} | {}", showId, showMessage);

    String url = showMessage.getUrl();
    try {
      HtmlPage htmlPage = webFetcher.fetchPage(url);

      List<KeyValue<KissEpisodePageKey, KissEpisodePageMessage>> episodeObjectList = extractor
          .extract(new KeyValue<>(showMessage, htmlPage));

      Set<Integer> skipEpisodeNumbers = new HashSet<>();
      boolean skipAllEpisodes = false;
      // if the show has a "skipEpisodeString", parse it to find out what episodes to skip
      if (showMessage.getSkipEpisodeString() != null && !showMessage.getSkipEpisodeString()
          .isEmpty()) {
        skipAllEpisodes = showMessage.getSkipEpisodeString().trim().equalsIgnoreCase("all");
        skipEpisodeNumbers = parseSkipEpisodeNumbers(
            showMessage.getSkipEpisodeString(),
            episodeObjectList.get(episodeObjectList.size() - 1).value.getEpisodeNumber());
      }

      int numberEpisodesPublished = 0;
      int numberEpisodesSkipped = 0;
      int numberEpisodesAlreadyDownloaded = 0;

      // send all messages in order
      for (KeyValue<KissEpisodePageKey, KissEpisodePageMessage> episodePagePair : episodeObjectList) {
        if (skipAllEpisodes || skipEpisodeNumbers
            .contains(episodePagePair.value.getEpisodeNumber())) {
          numberEpisodesSkipped++;
          downloadedEpisodePublisher.send(createDownloadedPair(episodePagePair));
          continue;
        }

        if (!episodeHasBeenDownloaded(episodePagePair)) {
          numberEpisodesPublished++;
          episodeMessagePublisher.send(episodePagePair);
          trySleep(200);
        } else {
          numberEpisodesAlreadyDownloaded++;
        }
      }
      log.info("Show: {}; "
              + "Found {} episodes; "
              + "Published: {}; "
              + "Skipped {} for initPolicy; "
              + "{} already downloaded episodes ignored.", showMessage.getName(),
          episodeObjectList.size(),
          numberEpisodesPublished, numberEpisodesSkipped, numberEpisodesAlreadyDownloaded);

    } catch (IOException | URISyntaxException | IllegalArgumentException e) {
      log.error("Error Processing show page");
      e.printStackTrace();
    }
  }

  private KeyValue<DownloadedEpisodeKey, DownloadedEpisodeMessage> createDownloadedPair(
      KeyValue<KissEpisodePageKey, KissEpisodePageMessage> pair) {
    return new KeyValue<>(
        DownloadedEpisodeKey.newBuilder()
            .setShowName(pair.value.getShowName())
            .setEpisodeName(pair.value.getEpisodeName())
            .setEpisodeNumber(pair.value.getEpisodeNumber())
            .setSeasonNumber(pair.value.getSeasonNumber())
            .setSubOrDub(pair.value.getSubOrDub())
            .build(),
        DownloadedEpisodeMessage.newBuilder()
            .setRetrieveTime(DateTime.now())
            .build());
  }

  private Set<Integer> parseSkipEpisodeNumbers(String skipEpisodeString, int highestEpNumber) {
    Set<Integer> skipEpisodeNumbers = new HashSet<>();

    if (skipEpisodeString.trim().equalsIgnoreCase("all")) {
      for (int i = 0; i <= highestEpNumber; i++) {
        skipEpisodeNumbers.add(i);
      }
    } else {
      String[] episodeRanges = skipEpisodeString.split(",");
      Arrays.asList(episodeRanges)
          .forEach(range -> skipEpisodeNumbers.addAll(parseRange(range, highestEpNumber)));
    }

    return skipEpisodeNumbers;
  }

  private Set<Integer> parseRange(String range, int highestEpNumber) {
    Set<Integer> output = new HashSet<>();

    log.info("Parsing range '{}'", range);
    Matcher matcher = PARSE_RANGE_PATTERN.matcher(range);
    if (matcher.find()) {
      int startEp = Integer.parseInt(matcher.group(1));
      if (matcher.group(2) != null && matcher.group(2).equals("-")) {
        int endEp = highestEpNumber;
        if (matcher.group(3) != null && !matcher.group(3).isEmpty()) {
          // range with start and end
          endEp = Integer.parseInt(matcher.group(3));
        }
        for (int i = startEp; i <= endEp; i++) {
          output.add(i);
        }
      } else {
        // single episode number
        output.add(startEp);
      }
    } else {
      log.warn("Unable to parse episode number range '{}'", range);
    }

    return output;
  }

  private boolean episodeHasBeenDownloaded(
      KeyValue<KissEpisodePageKey, KissEpisodePageMessage> episodePagePair) {
    return downloadedEpisodeStore.get(DownloadedEpisodeKey.newBuilder()
        .setShowName(episodePagePair.value.getShowName())
        .setSubOrDub(episodePagePair.value.getSubOrDub())
        .setSeasonNumber(episodePagePair.value.getSeasonNumber())
        .setEpisodeNumber(episodePagePair.value.getEpisodeNumber())
        .setEpisodeName(episodePagePair.value.getEpisodeName())
        .build()) != null;
  }

  @Override
  public void close() {

  }

  private static void trySleep(long ms) {
    try {
      Thread.sleep(ms);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }


}
