package com.brokencircuits.kissad.streamshowfetch.config;

import com.brokencircuits.kissad.kissweb.KissWebFetcher;
import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.InteractivePage;
import com.gargoylesoftware.htmlunit.ScriptException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.javascript.JavaScriptErrorListener;
import java.net.MalformedURLException;
import java.net.URL;
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
    client.setJavaScriptErrorListener(new JavaScriptErrorListener() {
      @Override
      public void scriptException(InteractivePage interactivePage, ScriptException e) {

      }

      @Override
      public void timeoutError(InteractivePage interactivePage, long l, long l1) {

      }

      @Override
      public void malformedScriptURL(InteractivePage interactivePage, String s,
          MalformedURLException e) {

      }

      @Override
      public void loadScriptError(InteractivePage interactivePage, URL url, Exception e) {

      }
    });

    return client;
  }

  @Bean
  KissWebFetcher webFetcher(WebClient webClient) {
    return new KissWebFetcher(webClient);
  }
}
