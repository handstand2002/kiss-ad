package com.brokencircuits.kissad.hsshowfetch.config;

import com.brokencircuits.kissad.Extractor;
import com.brokencircuits.kissad.hsshowfetch.domain.EpisodeDetails;
import com.brokencircuits.kissad.hsshowfetch.domain.MagnetUrl;
import com.gargoylesoftware.htmlunit.html.DomNode;
import com.gargoylesoftware.htmlunit.html.DomNodeList;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class ExtractorConfig {

  private static final Pattern SHOW_ID_PATTERN = Pattern.compile("var hs_showid = (\\d+);");
  private static final Pattern FIND_LINK_QUALITY_PATTERN = Pattern
      .compile("\\d+-(\\d+)p", Pattern.CASE_INSENSITIVE);
  private final static Pattern episodeNumberPattern = Pattern.compile("^\\d+$");


  @Bean
  Extractor<String, Long> showIdExtractor() {
    return input -> {
      Matcher matcher = SHOW_ID_PATTERN.matcher(input);
      if (matcher.find()) {
        String showId = matcher.group(1);
        return Long.valueOf(showId);
      } else {
        throw new IllegalArgumentException("Could not extract showId from page");
      }
    };
  }

  @Bean
  Extractor<HtmlPage, Collection<EpisodeDetails>> episodeDetailExtractor() {
    return page -> {
      DomNodeList<DomNode> domNodes = page.getBody().querySelectorAll(".rls-info-container");

      List<EpisodeDetails> allEpisodes = new ArrayList<>();
      domNodes.forEach(episodeNode -> {
        String episodeNum = episodeNode.getAttributes().getNamedItem("id").getNodeValue();
        DomNodeList<DomNode> qualityLinks = episodeNode.querySelectorAll(".rls-link");

        if (episodeNumberPattern.matcher(episodeNum).find()) {
          EpisodeDetails details = new EpisodeDetails(Long.parseLong(episodeNum));
          qualityLinks
              .forEach(link -> details.getUrlList().add(episodeMagnetUrlFromLinkNode(link)));
          allEpisodes.add(details);
        } else {
          log.warn("Unable to parse episode number: {}", episodeNum);
        }
      });

      return allEpisodes;
    };
  }

  private MagnetUrl episodeMagnetUrlFromLinkNode(DomNode link) {

    String qualityString = link.getAttributes().getNamedItem("id").getNodeValue();
    Matcher matcher = FIND_LINK_QUALITY_PATTERN.matcher(qualityString);
    int quality = -1;
    if (matcher.find()) {
      quality = Integer.parseInt(matcher.group(1));
    }
    String href = link.querySelector(".hs-magnet-link a").getAttributes().getNamedItem("href")
        .getNodeValue();
    return new MagnetUrl(quality, href);
  }
}
