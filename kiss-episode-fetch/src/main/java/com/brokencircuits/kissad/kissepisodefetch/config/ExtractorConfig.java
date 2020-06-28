package com.brokencircuits.kissad.kissepisodefetch.config;

import com.brokencircuits.kissad.Extractor;
import com.gargoylesoftware.htmlunit.html.DomNode;
import com.gargoylesoftware.htmlunit.html.HtmlInlineFrame;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class ExtractorConfig {

  @Bean
  Extractor<HtmlPage, String> episodeIframeExtractor(
      @Value("${kiss.episode.iframe-selector: iframe#my_video_1}") String iframeSelector) {
    return page -> {

      log.info("Fetching external iframe from page {}", page.getBaseURL().toString());
      DomNode domNode = page.getBody().querySelector(iframeSelector);
      if (domNode != null && domNode.getClass().equals(HtmlInlineFrame.class)) {
        String iframeSrc = ((HtmlInlineFrame) domNode).getSrcAttribute();
        log.info("iFrame source: {}", iframeSrc);
        return iframeSrc;
      }

      log.info("Could not find iFrame");
      return null;
    };
  }
}
