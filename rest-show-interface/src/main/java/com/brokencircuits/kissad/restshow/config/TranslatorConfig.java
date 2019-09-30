package com.brokencircuits.kissad.restshow.config;

import avro.shaded.com.google.common.collect.Maps;
import com.brokencircuits.kissad.Translator;
import com.brokencircuits.kissad.messages.ShowMessage;
import com.brokencircuits.kissad.messages.SourceName;
import com.brokencircuits.kissad.restshow.rest.domain.ShowObject;
import com.brokencircuits.kissad.restshow.rest.domain.ShowSource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.KeyValue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class TranslatorConfig {

  @Bean
  Translator<ShowObject, KeyValue<Long, ShowMessage>> showLocalToMsgTranslator(
      Translator<Collection<ShowSource>, Map<String, String>> showSourceTranslator) {
    return input -> new KeyValue<>(
        input.getShowId(),
        ShowMessage.newBuilder()
            .setSeason(input.getSeason())
            .setIsActive(input.getIsActive())
            .setTitle(input.getTitle())
            .setSkipEpisodeString(input.getInitialSkipEpisodeString())
            .setSources(showSourceTranslator.translate(input.getSources()))
            .build());
  }

  @Bean
  Translator<KeyValue<Long, ShowMessage>, ShowObject> showMsgToLocalTranslator(
      Translator<Map<String, String>, Collection<ShowSource>> showSourceTranslator) {
    return pair -> ShowObject.builder()
        .season(pair.value.getSeason())
        .title(pair.value.getTitle())
        .showId(pair.key)
        .sources(showSourceTranslator.translate(pair.value.getSources()))
        .build();
  }

  @Bean
  Translator<Collection<ShowSource>, Map<String, String>> showSourceObjToMsgTranslator() {
    return input -> {
      Map<String, String> output = Maps.newHashMap();
      for (ShowSource sourceObj : input) {
        output.put(sourceObj.getSourceName().toString(), sourceObj.getUrl());
      }
      return output;
    };
  }

  @Bean
  Translator<Map<String, String>, Collection<ShowSource>> showSourceMsgToObjTranslator() {
    return input -> {
      Collection<ShowSource> output = new ArrayList<>();
      for (Entry<String, String> entry : input.entrySet()) {
        String sourceName = entry.getKey();
        String url = entry.getValue();
        output.add(ShowSource.builder()
            .sourceName(SourceName.valueOf(sourceName))
            .url(url)
            .build());
      }
      return output;
    };
  }

}
