package com.brokencircuits.kissad.hsshowfetch.config;

import avro.shaded.com.google.common.collect.Lists;
import com.brokencircuits.kissad.Extractor;
import com.brokencircuits.kissad.hsshowfetch.domain.EpisodeDetails;
import com.brokencircuits.kissad.hsshowfetch.domain.MagnetUrl;
import com.gargoylesoftware.htmlunit.html.DomNode;
import com.gargoylesoftware.htmlunit.html.DomNodeList;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ExtractorConfig {

  private static final Pattern SHOW_ID_PATTERN = Pattern
      .compile("var hs_showid = (\\d+);");
  private static final Pattern FIND_LINK_QUALITY_PATTERN = Pattern
      .compile("\\d+-(\\d+)p", Pattern.CASE_INSENSITIVE);

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

      List<EpisodeDetails> allEpisodes = Lists.newArrayList();
      domNodes.forEach(episodeNode -> {
        String episodeNum = episodeNode.getAttributes().getNamedItem("id").getNodeValue();
        DomNodeList<DomNode> qualityLinks = episodeNode.querySelectorAll(".rls-link");

        EpisodeDetails details = new EpisodeDetails(Long.valueOf(episodeNum));
        qualityLinks.forEach(link -> details.getUrlList().add(episodeMagnetUrlFromLinkNode(link)));
        allEpisodes.add(details);
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
