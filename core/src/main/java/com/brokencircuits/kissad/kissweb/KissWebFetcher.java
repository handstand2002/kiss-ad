package com.brokencircuits.kissad.kissweb;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import java.io.IOException;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class KissWebFetcher {

  final private WebClient webClient;

  public HtmlPage fetchPage(String url) throws IOException {
    // TODO: Check if page is cloudflare page or kiss page
    webClient.getPage(url);
    webClient.waitForBackgroundJavaScript(10000);

    return webClient.getPage(url);
  }
}
