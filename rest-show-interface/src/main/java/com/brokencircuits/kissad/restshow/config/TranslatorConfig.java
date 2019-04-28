package com.brokencircuits.kissad.restshow.config;

import com.brokencircuits.kissad.Translator;
import com.brokencircuits.kissad.messages.KissShowMessage;
import com.brokencircuits.kissad.restshow.rest.domain.ShowObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.KeyValue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class TranslatorConfig {

  @Bean
  Translator<ShowObject, KissShowMessage> showLocalToMessage() {

    return input -> {
      log.info("Translating from {}", input);
      return KissShowMessage.newBuilder()
          .setSeasonNumber(input.getSeasonNumber())
          .setIsActive(input.getIsActive())
          .setName(input.getShowName())
          .setSkipEpisodeString(input.getInitialSkipEpisodeString())
          .setUrl(input.getShowUrl())
          .build();
    };
  }

  @Bean
  Translator<KeyValue<Long, KissShowMessage>, ShowObject> showMessageToLocal() {
    return pair -> ShowObject.builder()
        .seasonNumber(pair.value.getSeasonNumber())
        .showName(pair.value.getName())
        .showUrl(pair.value.getUrl())
        .showId(pair.key)
        .build();
  }
}
