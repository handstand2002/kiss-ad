package com.brokencircuits.kissad.kissweb;

import com.brokencircuits.kissad.WebFetcher;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class KissWebFetcher implements WebFetcher {

  final private WebClient webClient;

  public HtmlPage fetchPage(String url) throws IOException {
    HtmlPage page = webClient.getPage(url);
    log.info("Page size: {}", page.asXml().length());


    if (page.asXml().length() < 12000) {
      log.info("Size of page is very small, assuming it is cloudflare page. Will reload page soon");
      webClient.waitForBackgroundJavaScript(10000);

      page = webClient.getPage(url);
      log.info("Page size: {}", page.asXml().length());
    }
    webClient.close();

    return page;
  }
}
