package com.brokencircuits.kissad.hsshowfetch.rest;

import com.brokencircuits.kissad.Extractor;
import com.brokencircuits.kissad.hsshowfetch.domain.EpisodeDetails;
import com.brokencircuits.kissad.hsshowfetch.domain.MagnetUrl;
import com.brokencircuits.kissad.hsshowfetch.fetch.HsEpisodeFetcher;
import com.brokencircuits.kissad.kafka.KeyValueStore;
import com.brokencircuits.kissad.kafka.Publisher;
import com.brokencircuits.kissad.messages.DownloadType;
import com.brokencircuits.kissad.messages.EpisodeKey;
import com.brokencircuits.kissad.messages.EpisodeLink;
import com.brokencircuits.kissad.messages.EpisodeLinks;
import com.brokencircuits.kissad.messages.ShowMessage;
import com.brokencircuits.kissad.messages.SourceName;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.springframework.core.task.TaskExecutor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class CommandController {

  private final TaskExecutor applicationTaskExecutor;
  private final KeyValueStore<Long, ShowMessage> showStore;
  private final WebClient webClient;
  private final Extractor<String, Long> showIdExtractor;
  private final HsEpisodeFetcher hsEpisodeFetcher;
  private final Publisher<EpisodeKey, EpisodeLinks> episodeMessagePublisher;

  @GetMapping(path = "/checkNewEpisodes")
  public String checkNewEpisodes() {
    applicationTaskExecutor.execute(checkNewEpisodesRunner());
    return "STARTED";
  }

  private Runnable checkNewEpisodesRunner() {
    return () -> {
      long numShows = 0;
      KeyValueIterator<Long, ShowMessage> iter = showStore.all();
      while (iter.hasNext()) {
        numShows++;
        KeyValue<Long, ShowMessage> entry = iter.next();
        if (entry.value.getIsActive() && entry.value.getSources()
            .containsKey(SourceName.HORRIBLESUBS.toString())) {

          log.info("Processing show '{}'", entry.value.getTitle());
          processEntry(entry);

        } else {
          log.info("Show {} does not have HS URL, skipping", entry.value.getTitle());
        }
      }
      log.info("{} shows in cache;", numShows);
    };
  }

  private void processEntry(KeyValue<Long, ShowMessage> showPair) {

    String hsUrl = showPair.value.getSources().get(SourceName.HORRIBLESUBS.toString());

    try {
      String htmlPage = fetchPageFromUrl(hsUrl);
      Long showId = showIdExtractor.extract(htmlPage);
      Collection<EpisodeDetails> episodes = hsEpisodeFetcher.getEpisodes(showId);
      episodes.forEach(ep -> episodeMessagePublisher.send(
          EpisodeKey.newBuilder()
              .setEpisodeNumber(ep.getEpisodeNumber())
              .setShow(showPair.value)
              .build(),
          EpisodeLinks.newBuilder()
              .setLinks(convertUrlList(ep.getUrlList()))
              .build()));

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private List<EpisodeLink> convertUrlList(List<MagnetUrl> urlList) {
    List<EpisodeLink> output = new ArrayList<>();
    for (MagnetUrl magnetUrl : urlList) {
      output.add(EpisodeLink.newBuilder()
          .setType(DownloadType.MAGNET)
          .setQuality(magnetUrl.getQuality())
          .setUrl(magnetUrl.getUrl())
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
