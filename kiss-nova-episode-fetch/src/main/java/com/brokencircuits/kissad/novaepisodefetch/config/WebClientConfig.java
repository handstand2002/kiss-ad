package com.brokencircuits.kissad.novaepisodefetch.config;

import com.brokencircuits.kissad.novaepisodefetch.nova.NovaApiClient;
import feign.Feign;
import feign.Logger.Level;
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
  NovaApiClient novaApiClient(@Value("${nova.api.root-url}") String url) {
    return Feign.builder()
        .client(new OkHttpClient())
        .encoder(new GsonEncoder())
        .decoder(new GsonDecoder())
        .logger(new Slf4jLogger(NovaApiClient.class))
        .logLevel(Level.FULL)
        .target(NovaApiClient.class, url);
  }

}
