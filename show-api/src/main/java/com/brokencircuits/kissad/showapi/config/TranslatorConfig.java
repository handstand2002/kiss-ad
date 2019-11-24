package com.brokencircuits.kissad.showapi.config;

import com.brokencircuits.kissad.Translator;
import com.brokencircuits.kissad.kafka.ByteKey;
import com.brokencircuits.kissad.messages.ShowMsg;
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
  Translator<ShowObject, KeyValue<ByteKey<ShowMsgKey>, ShowMsg>> showLocalToMsgTranslator() {
    return input -> {
      ShowMsgKey key = ShowMsgKey.newBuilder().setShowId(input.getShowId()).build();
      ShowMsgValue value = ShowMsgValue.newBuilder()
          .setTitle(input.getTitle())
          .setSeason(input.getSeason())
          .setSources(convertSources(input.getSources()))
          .setIsActive(input.getIsActive())
          .setReleaseScheduleCron(input.getReleaseScheduleCron())
          .setSkipEpisodeString(input.getInitialSkipEpisodeString())
          .setEpisodeNamePattern(input.getEpisodeNamePattern())
          .setFolderName(input.getFolderName())
          .setMessageId(Uuid.randomUUID())
          .build();
      return new KeyValue<>(
          new ByteKey<>(key),
          ShowMsg.newBuilder().setKey(key).setValue(value).build());
    };
  }

  private static Map<String, String> convertSources(Collection<ShowSource> sources) {
    Map<String, String> outputMap = new HashMap<>();
    sources.forEach(source -> outputMap.put(source.getSourceName().name(), source.getUrl()));
    return outputMap;
  }

  @Bean
  Translator<KeyValue<ByteKey<ShowMsgKey>, ShowMsg>, ShowObject> showMsgToLocalTranslator() {
    return pair -> ShowObject.builder()
        .title(pair.value.getValue().getTitle())
        .season(pair.value.getValue().getSeason())
        .showId(pair.value.getKey().getShowId())
        .isActive(pair.value.getValue().getIsActive())
        .initialSkipEpisodeString(pair.value.getValue().getSkipEpisodeString())
        .releaseScheduleCron(pair.value.getValue().getReleaseScheduleCron())
        .sources(convertSources(pair.value.getValue().getSources()))
        .episodeNamePattern(pair.value.getValue().getEpisodeNamePattern())
        .folderName(pair.value.getValue().getFolderName())
        .build();
  }

  private static Collection<ShowSource> convertSources(Map<String, String> sources) {
    Collection<ShowSource> outputCollection = Sets.newHashSet();
    sources.forEach((sourceName, url) -> outputCollection
        .add(ShowSource.builder().sourceName(SourceName.valueOf(sourceName)).url(url).build()));

    return outputCollection;
  }


}
