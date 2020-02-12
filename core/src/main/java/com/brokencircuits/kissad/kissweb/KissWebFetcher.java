package com.brokencircuits.kissad.kissweb;

import com.brokencircuits.kissad.WebFetcher;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.WebResponse;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import java.io.IOException;
import java.net.URL;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class KissWebFetcher implements WebFetcher {

  final private WebClient webClient;

  public WebResponse fetchResource(String uri) throws IOException {
    return webClient.getWebConnection().getResponse(new WebRequest(new URL(uri)));
  }

  public HtmlPage fetchPage(String url) throws IOException {
    HtmlPage page = webClient.getPage(url);
    log.info("Page size: {}", page.getWebResponse().getContentAsString().length());

    Pattern isCloudflarePattern = Pattern.compile("Just a moment");
    if (isCloudflarePattern.matcher(page.getTitleText()).find()) {
//    if (page.asXml().length() < 12000) {
      log.info("Appears to be a cloudflare page. Will reload page soon");
      webClient.waitForBackgroundJavaScript(10000);

      page = webClient.getPage(url);
      log.info("Page size: {}", page.getWebResponse().getContentAsString().length());
    }
    webClient.close();

    return page;
  }
}
