package com.brokencircuits.kissad.fetcher;

import com.brokencircuits.kissad.Extractor;
import com.brokencircuits.kissad.domain.CheckShowResult;
import com.brokencircuits.kissad.domain.DownloadTypeDto;
import com.brokencircuits.kissad.domain.EpisodeLinkDto;
import com.brokencircuits.kissad.domain.RequestEpisode;
import com.brokencircuits.kissad.domain.RequestEpisodeOperation;
import com.brokencircuits.kissad.domain.RequestEpisodeResult;
import com.brokencircuits.kissad.domain.ShowDto;
import com.brokencircuits.kissad.domain.fetcher.ShowEpisodeResponse;
import com.brokencircuits.kissad.domain.fetcher.SpEpisode;
import com.brokencircuits.kissad.domain.fetcher.SpEpisodeDownloadEntry;
import com.brokencircuits.kissad.domain.rest.SourceName;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class SpFetcher {

  public static final String SOURCE_IDENTIFIER = "SUBSPLEASE";
  private final SourceName acceptSource = SourceName.SUBSPLEASE;
  private final WebClient webClient;
  private final Extractor<String, Long> showIdExtractor;
  private final SpShowEpisodeClient feignSpShowEpisodeFetcher;
  private final RequestEpisodeOperation notifyOfEpisode;

  public CheckShowResult process(ShowDto show) {
    log.info("Processing show: {}", show);

    if (!show.getSourceName().equals(acceptSource.name())) {
      log.warn("Fetcher received show for wrong source: {}", show);
      return new CheckShowResult(0, true);
    }

    String url = show.getUrl();
    try {
      String htmlPage = fetchPageFromUrl(url);
      Long showId = showIdExtractor.extract(htmlPage);

      ShowEpisodeResponse episodeResponse = feignSpShowEpisodeFetcher
          .findAll(String.valueOf(showId));

      List<RequestEpisodeResult> results = episodeResponse.getEpisode().entrySet().stream()
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
              return convertEpisodeObj(e.getValue(), epNumber, show);
            } catch (Exception exception) {
              log.error("Unable to process episode {} | {} due to error", e.getKey(), e.getValue(),
                  exception);
              return null;
            }
          })
          .filter(Objects::nonNull)
          .sorted(Comparator.comparingLong(RequestEpisode::getEpisodeNumber))
          .map(notifyOfEpisode::run)
          .collect(Collectors.toList());
      int completedEpisodes = results.stream().mapToInt(r -> r.isDownloadComplete() ? 1 : 0).sum();

      return new CheckShowResult(completedEpisodes, false);

    } catch (Exception e) {
      log.error("Unable to process {} due to Exception", show, e);
      return new CheckShowResult(0, true);
    }
  }

  private RequestEpisode convertEpisodeObj(
      SpEpisode ep, Long epNumber, ShowDto showKey) {
    return RequestEpisode.builder()
        .episodeNumber(Math.toIntExact(epNumber))
        .showId(showKey.getId())
        .links(convertUrlList(ep.getDownloads()))
        .build();
//    EpisodeMsgKey key = EpisodeMsgKey.newBuilder()
//        .setEpisodeNumber(epNumber)
//        .setShowId(showKey)
//        .build();
//    EpisodeMsgValue value = EpisodeMsgValue.newBuilder()
//        .setDownloadedQuality(0)
//        .setLatestLinks(convertUrlList(ep.getDownloads()))
//        .setDownloadTime(null)
//        .setMessageId(Uuid.randomUUID())
//        .build();
//    return KeyValue.of(key, EpisodeMsg.newBuilder().setKey(key).setValue(value).build());
  }

  private List<EpisodeLinkDto> convertUrlList(List<SpEpisodeDownloadEntry> urlList) {
    List<EpisodeLinkDto> output = new ArrayList<>();
    for (SpEpisodeDownloadEntry downloadEntry : urlList) {
      output.add(new EpisodeLinkDto(downloadEntry.getRes(), DownloadTypeDto.MAGNET,
          downloadEntry.getMagnet()));
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
