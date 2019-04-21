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
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.processor.Processor;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ShowMessageProcessor implements Processor<Long, KissShowMessage> {


  final private KissWebFetcher webFetcher;
  final private Publisher<KissEpisodePageKey, KissEpisodePageMessage> episodeMessagePublisher;
  final private EpisodeMessageExtractor extractor;

  @Value("${messaging.stores.downloaded-episode}")
  private String downloadedEpisodeStoreName;

  private ReadOnlyKeyValueStore<DownloadedEpisodeKey, DownloadedEpisodeMessage> downloadedEpisodeStore;

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

      int numberEpisodesPublished = 0;
      // send all messages in order
      for (KeyValue<KissEpisodePageKey, KissEpisodePageMessage> episodePagePair : episodeObjectList) {
        if (!episodeHasBeenDownloaded(episodePagePair)) {
          numberEpisodesPublished++;
          episodeMessagePublisher.send(episodePagePair);
          trySleep(200);
        }
      }
      log.info("Found {} new episodes; {} already downloaded episodes ignored.",
          numberEpisodesPublished, episodeObjectList.size() - numberEpisodesPublished);

    } catch (IOException | URISyntaxException | IllegalArgumentException e) {
      log.error("Error Processing show page: {}", e.getMessage());
    }
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
