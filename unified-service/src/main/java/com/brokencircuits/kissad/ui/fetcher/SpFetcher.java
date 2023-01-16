package com.brokencircuits.kissad.ui.fetcher;

import com.brokencircuits.download.messages.DownloadType;
import com.brokencircuits.kissad.Extractor;
import com.brokencircuits.kissad.messages.*;
import com.brokencircuits.kissad.ui.fetcher.domain.ShowEpisodeResponse;
import com.brokencircuits.kissad.ui.fetcher.domain.SpEpisode;
import com.brokencircuits.kissad.ui.fetcher.domain.SpEpisodeDownloadEntry;
import com.brokencircuits.kissad.util.ByteKey;
import com.brokencircuits.kissad.util.KeyValue;
import com.brokencircuits.kissad.util.Uuid;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

@Slf4j
@RequiredArgsConstructor
@Component
public class SpFetcher {

  public static final String SOURCE_IDENTIFIER = "SUBSPLEASE";
  private final SourceName acceptSource = SourceName.SUBSPLEASE;
  private final WebClient webClient;
  private final Extractor<String, Long> showIdExtractor;
  private final SpShowEpisodeClient feignSpShowEpisodeFetcher;
  private final Consumer<EpisodeMsg> notifyOfEpisode;

  public void process(ByteKey<ShowMsgKey> key, ShowMsg msg) {
    log.info("Processing show: {} | {}", key, msg);
    String url = msg.getValue().getSources().get(acceptSource.toString());
    try {
      String htmlPage = fetchPageFromUrl(url);
      Long showId = showIdExtractor.extract(htmlPage);

      ShowEpisodeResponse episodeResponse = feignSpShowEpisodeFetcher
          .findAll(String.valueOf(showId));

      episodeResponse.getEpisode().entrySet().stream()
          .map(e -> {
            try {
              String episodeNumberString = e.getValue().getEpisode();
              if (episodeNumberString.contains("v") || episodeNumberString.contains("V")) {
                int lastIndex = episodeNumberString.indexOf("v");
                if (lastIndex == -1) {
                  lastIndex = episodeNumberString.indexOf("V");
                }
                episodeNumberString = episodeNumberString.substring(0, lastIndex);
              }
              Long epNumber = Long.parseLong(episodeNumberString);
              return convertEpisodeObj(e.getValue(), epNumber, msg.getKey());
            } catch (Exception exception) {
              log.error("Unable to process episode {} | {} due to error", e.getKey(), e.getValue(), exception);
              return null;
            }
          })
          .filter(Objects::nonNull)
          .sorted(Comparator.comparingLong(kv -> kv.getValue().getKey().getEpisodeNumber()))
          .forEach(kv -> notifyOfEpisode.accept(kv.getValue()));

    } catch (Exception e) {
      log.error("Unable to process {} due to Exception", msg, e);
    }
  }

  private KeyValue<ByteKey<EpisodeMsgKey>, EpisodeMsg> convertEpisodeObj(
      SpEpisode ep, Long epNumber, ShowMsgKey showKey) {
    EpisodeMsgKey key = EpisodeMsgKey.newBuilder()
        .setEpisodeNumber(epNumber)
        .setShowId(showKey)
        .build();
    EpisodeMsgValue value = EpisodeMsgValue.newBuilder()
        .setDownloadedQuality(0)
        .setLatestLinks(convertUrlList(ep.getDownloads()))
        .setDownloadTime(null)
        .setMessageId(Uuid.randomUUID())
        .build();
    return KeyValue.of(
        ByteKey.from(key),
        EpisodeMsg.newBuilder().setKey(key).setValue(value).build());
  }

  private List<EpisodeLink> convertUrlList(List<SpEpisodeDownloadEntry> urlList) {
    List<EpisodeLink> output = new ArrayList<>();
    for (SpEpisodeDownloadEntry downloadEntry : urlList) {
      output.add(EpisodeLink.newBuilder()
          .setType(DownloadType.MAGNET)
          .setQuality(downloadEntry.getRes())
          .setUrl(downloadEntry.getMagnet())
          .build());
    }
    return output;
  }

  private String fetchPageFromUrl(String url) throws IOException {
    HtmlPage page = webClient.getPage(url);
    log.info("Page size: {}", page.getWebResponse().getContentAsString().length());
    webClient.close();
    return page.getWebResponse().getContentAsString();
  }
}
