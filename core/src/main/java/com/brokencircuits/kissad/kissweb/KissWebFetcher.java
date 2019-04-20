package com.brokencircuits.kissad.kissweb;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class KissWebFetcher {

  final private WebClient webClient;

  public HtmlPage fetchPage(String url) throws IOException {
    // TODO: Check if page is cloudflare page or kiss page
    HtmlPage page = webClient.getPage(url);
    log.info("Page size: {}", page.asXml().length());

    // must be cloudflare page
    if (page.asXml().length() < 12000) {
      webClient.waitForBackgroundJavaScript(10000);

      page = webClient.getPage(url);
      log.info("Page size: {}", page.asXml().length());
    }

    return page;
  }
}
