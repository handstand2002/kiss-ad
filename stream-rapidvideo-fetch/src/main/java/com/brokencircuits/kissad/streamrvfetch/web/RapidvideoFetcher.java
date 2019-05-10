package com.brokencircuits.kissad.streamrvfetch.web;

import com.brokencircuits.kissad.WebFetcher;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class RapidvideoFetcher implements WebFetcher {

  final private WebClient webClient;

  public HtmlPage fetchPage(String url) throws IOException {
    HtmlPage page = webClient.getPage(url);
    log.info("Page size: {}", page.getWebResponse().getContentAsString().length());

    return page;
  }
}
