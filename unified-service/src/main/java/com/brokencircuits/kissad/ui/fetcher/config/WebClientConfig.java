package com.brokencircuits.kissad.ui.fetcher.config;

import com.brokencircuits.kissad.kissweb.CloudflareWebFetcher;
import com.brokencircuits.kissad.ui.fetcher.SpShowEpisodeClient;
import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import feign.Feign;
import feign.Logger;
import feign.gson.GsonDecoder;
import feign.gson.GsonEncoder;
import feign.okhttp.OkHttpClient;
import feign.slf4j.Slf4jLogger;
import org.springframework.beans.factory.annotation.Value;
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

  @Bean
  public SpShowEpisodeClient feignSpShowEpisodeFetcher(@Value("${org.subsplease.url}") String spUrl) {
    return Feign.builder()
        .client(new OkHttpClient())
        .encoder(new GsonEncoder())
        .decoder(new GsonDecoder())
        .logger(new Slf4jLogger(SpShowEpisodeClient.class))
        .logLevel(Logger.Level.FULL)
        .target(SpShowEpisodeClient.class, spUrl);
  }
}
