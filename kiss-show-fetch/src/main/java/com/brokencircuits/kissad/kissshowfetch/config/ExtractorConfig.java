package com.brokencircuits.kissad.kissshowfetch.config;

import com.brokencircuits.kissad.Extractor;
import com.brokencircuits.kissad.kissshowfetch.domain.EpisodePageDetails;
import com.gargoylesoftware.htmlunit.html.DomNode;
import com.gargoylesoftware.htmlunit.html.DomNodeList;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import java.net.URL;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ExtractorConfig {

  private static final String EPISODE_ANCHOR_SELECTOR = "div.barContent.episodeList table a";
  private static final Pattern EPISODE_NUMBER_PATTERN = Pattern.compile("(\\d{3})$");

  @Bean
  Extractor<HtmlPage, List<EpisodePageDetails>> episodePageDetailExtractor() {
    return page -> {

      URL pageUrl = page.getBaseURL();
      String urlString = pageUrl.toString();
      String rootUrl = urlString.substring(0, urlString.indexOf(pageUrl.getPath()));

      DomNodeList<DomNode> linkNodes = page.querySelectorAll(EPISODE_ANCHOR_SELECTOR);
      return linkNodes.stream()
          .map(node -> {
            String episodeUrl = rootUrl + ((HtmlAnchor) node).getHrefAttribute();
            String episodeName = node.getTextContent().trim();
            Integer episodeNumber = null;
            Matcher episodeNumMatcher = EPISODE_NUMBER_PATTERN.matcher(episodeName);
            if (episodeNumMatcher.find()) {
              episodeNumber = Integer.parseInt(episodeNumMatcher.group(1));
            }
            return new EpisodePageDetails(episodeUrl, episodeNumber, episodeName);
          })
          .sorted(Comparator.comparing(EpisodePageDetails::getEpisodeNumber))
          .collect(Collectors.toList());
    };
  }
}
