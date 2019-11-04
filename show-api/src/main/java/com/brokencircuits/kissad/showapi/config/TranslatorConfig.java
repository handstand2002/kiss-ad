package com.brokencircuits.kissad.showapi.config;

import com.brokencircuits.kissad.Translator;
import com.brokencircuits.kissad.messages.ShowMsgKey;
import com.brokencircuits.kissad.messages.ShowMsgValue;
import com.brokencircuits.kissad.messages.SourceName;
import com.brokencircuits.kissad.showapi.rest.domain.ShowObject;
import com.brokencircuits.kissad.showapi.rest.domain.ShowSource;
import com.brokencircuits.kissad.util.Uuid;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.utils.Sets;
import org.apache.kafka.streams.KeyValue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class TranslatorConfig {

  @Bean
  Translator<ShowObject, KeyValue<ShowMsgKey, ShowMsgValue>> showLocalToMsgTranslator() {
    return input -> new KeyValue<>(
        ShowMsgKey.newBuilder().setShowId(input.getShowId()).build(),
        ShowMsgValue.newBuilder()
            .setTitle(input.getTitle())
            .setSeason(input.getSeason())
            .setSources(convertSources(input.getSources()))
            .setIsActive(input.getIsActive())
            .setReleaseScheduleCron(input.getReleaseScheduleCron())
            .setSkipEpisodeString(input.getInitialSkipEpisodeString())
            .setEpisodeNamePattern(input.getEpisodeNamePattern())
            .setFolderName(input.getFolderName())
            .setMessageId(Uuid.randomUUID())
            .build());
  }

  private static Map<String, String> convertSources(Collection<ShowSource> sources) {
    Map<String, String> outputMap = new HashMap<>();
    sources.forEach(source -> outputMap.put(source.getSourceName().name(), source.getUrl()));
    return outputMap;
  }

  @Bean
  Translator<KeyValue<ShowMsgKey, ShowMsgValue>, ShowObject> showMsgToLocalTranslator() {
    return pair -> ShowObject.builder()
        .title(pair.value.getTitle())
        .season(pair.value.getSeason())
        .showId(pair.key.getShowId())
        .isActive(pair.value.getIsActive())
        .initialSkipEpisodeString(pair.value.getSkipEpisodeString())
        .releaseScheduleCron(pair.value.getReleaseScheduleCron())
        .sources(convertSources(pair.value.getSources()))
        .episodeNamePattern(pair.value.getEpisodeNamePattern())
        .folderName(pair.value.getFolderName())
        .build();
  }

  private static Collection<ShowSource> convertSources(Map<String, String> sources) {
    Collection<ShowSource> outputCollection = Sets.newHashSet();
    sources.forEach((sourceName, url) -> outputCollection
        .add(ShowSource.builder().sourceName(SourceName.valueOf(sourceName)).url(url).build()));

    return outputCollection;
  }


}
