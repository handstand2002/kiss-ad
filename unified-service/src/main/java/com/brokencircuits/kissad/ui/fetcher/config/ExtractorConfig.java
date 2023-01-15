package com.brokencircuits.kissad.ui.fetcher.config;

import com.brokencircuits.kissad.Extractor;
import com.brokencircuits.kissad.ui.fetcher.domain.EpisodeDetails;
import com.brokencircuits.kissad.ui.fetcher.domain.MagnetUrl;
import com.gargoylesoftware.htmlunit.html.DomNode;
import com.gargoylesoftware.htmlunit.html.DomNodeList;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class ExtractorConfig {

  //id="show-release-table" cellpadding="0" border="0" cellspacing="0" sid="134"
//  private static final Pattern SHOW_ID_PATTERN = Pattern.compile("var hs_showid = (\\d+);");
  private static final Pattern FIND_LINK_QUALITY_PATTERN = Pattern
      .compile("\\d+-(\\d+)p", Pattern.CASE_INSENSITIVE);
  private final static Pattern episodeNumberPattern = Pattern.compile("^\\d+$");

  @Bean
  Extractor<String, Long> showIdExtractor(
      @Value("${org.subsplease.show-page.get-show-id-pattern}") String getShowIdPatternString) {
    Pattern showIdPattern = Pattern.compile(getShowIdPatternString);
    return input -> {
      Matcher matcher = showIdPattern.matcher(input);
      if (matcher.find()) {
        String showId = matcher.group(1);
        return Long.valueOf(showId);
      } else {
        throw new IllegalArgumentException(String.format("Could not extract showId from page. "
            + "Searching for pattern '%s' within page: \n%s", showIdPattern, input));
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

  @Bean
  Extractor<String, Long> showEpisodeExtractor(
      @Value("${org.subsplease.show-page.get-episode-number-pattern}") String getEpisodeNumberPatternString) {
    Pattern getEpisodeNumberPattern = Pattern.compile(getEpisodeNumberPatternString);
    return episodeTitle -> {
      Matcher matcher = getEpisodeNumberPattern.matcher(episodeTitle);
      if (matcher.find()) {
        String showId = matcher.group(1);
        return Long.valueOf(showId);
      } else {
        throw new IllegalArgumentException(
            "Could not extract episode ID from title " + episodeTitle);
      }
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
