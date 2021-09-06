package com.brokencircuits.kissad.spshowfetch.fetch;

import com.brokencircuits.kissad.Extractor;
import com.brokencircuits.kissad.spshowfetch.domain.EpisodeDetails;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import java.io.IOException;
import java.util.Collection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SpEpisodeFetcher {

  private final WebClient webClient;
  private final Extractor<HtmlPage, Collection<EpisodeDetails>> episodeDetailExtractor;

  @Value("${org.subsplease.url}")
  private String url;
  @Value("${org.subsplease.get-show-endpoint}")
  private String endpoint;

  public Collection<EpisodeDetails> getEpisodes(long showId) throws Exception {
    String showEndpoint = endpoint.replace("<SHOWID>", String.valueOf(showId));

    HtmlPage page = fetchPageFromUrl(url + showEndpoint);

    return episodeDetailExtractor.extract(page);
  }

  private HtmlPage fetchPageFromUrl(String url) throws IOException {
    HtmlPage page = webClient.getPage(url);
    log.info("Page size: {}", page.getWebResponse().getContentAsString().length());
    webClient.close();
    return page;
  }

}
