package com.brokencircuits.kissad.hsshowfetch.config;

import com.brokencircuits.kissad.kissweb.CloudflareWebFetcher;
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
  CloudflareWebFetcher webFetcher(WebClient webClient) {
    return new CloudflareWebFetcher(webClient);
  }
}
