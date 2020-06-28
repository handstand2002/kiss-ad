package com.brokencircuits.kissad.kissshowfetch.config;

import com.brokencircuits.kissad.kissweb.KissWebFetcher;
import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebClientConfig {

  @Bean
  WebClient webClient() {
    final WebClient client = new WebClient(BrowserVersion.CHROME);
    client.getOptions().setCssEnabled(false);
    client.getOptions().setUseInsecureSSL(true);
    client.getOptions().setJavaScriptEnabled(true);
    client.getOptions().setThrowExceptionOnFailingStatusCode(false);
    client.getOptions().setThrowExceptionOnScriptError(false);
    client.getOptions().setRedirectEnabled(true);
    client.getCookieManager().setCookiesEnabled(true);
    client.getCache().setMaxSize(0);
    client.waitForBackgroundJavaScript(10000);
    client.setJavaScriptTimeout(10000);
    client.waitForBackgroundJavaScriptStartingBefore(10000);

    return client;
  }

  @Bean
  KissWebFetcher webFetcher(WebClient webClient) {
    return new KissWebFetcher(webClient);
  }
}
