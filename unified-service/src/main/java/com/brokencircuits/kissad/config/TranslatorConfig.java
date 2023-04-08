package com.brokencircuits.kissad.config;

import com.brokencircuits.kissad.Translator;
import com.brokencircuits.kissad.domain.rest.HsShowObject;
import com.brokencircuits.kissad.domain.rest.ShowObject;
import com.brokencircuits.kissad.messages.ShowMsg;
import com.brokencircuits.kissad.messages.ShowMsgKey;
import com.brokencircuits.kissad.messages.ShowMsgValue;
import com.brokencircuits.kissad.messages.SourceName;
import com.brokencircuits.kissad.util.KeyValue;
import com.brokencircuits.kissad.util.Uuid;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class TranslatorConfig {

  @Bean
  Translator<ShowObject, KeyValue<ShowMsgKey, ShowMsg>> showLocalToMsgTranslator() {
    return input -> {
      ShowMsgKey key = ShowMsgKey.newBuilder().setShowId(input.getShowId()).build();
      ShowMsgValue value = ShowMsgValue.newBuilder()
          .setTitle(input.getTitle())
          .setSeason(input.getSeason())
          .setSources(convertSources(input.getSourceName(), input.getUrl()))
          .setIsActive(input.getIsActive())
          .setReleaseScheduleCron(input.getReleaseScheduleCron())
          .setSkipEpisodeString(input.getInitialSkipEpisodeString())
          .setEpisodeNamePattern(input.getEpisodeNamePattern())
          .setFolderName(input.getFolderName())
          .setMessageId(Uuid.randomUUID())
          .build();
      return KeyValue.of(key, ShowMsg.newBuilder().setKey(key).setValue(value).build());
    };
  }

  private Map<String, String> convertSources(SourceName sourceName, String url) {
    Map<String, String> outputMap = new HashMap<>();
    outputMap.put(sourceName.name(), url);
    return outputMap;
  }

  @Bean
  Translator<HsShowObject, KeyValue<ShowMsgKey, ShowMsg>> hsShowLocalToMsgTranslator() {
    return input -> {
      ShowMsgKey key = ShowMsgKey.newBuilder().setShowId(input.getShowId()).build();
      ShowMsgValue value = ShowMsgValue.newBuilder()
          .setTitle(input.getTitle())
          .setSeason(input.getSeason())
          .setSources(convertSources(SourceName.HORRIBLESUBS, input.getHsUrl()))
          .setIsActive(input.getIsActive())
          .setReleaseScheduleCron(input.getReleaseScheduleCron())
          .setSkipEpisodeString(input.getInitialSkipEpisodeString())
          .setEpisodeNamePattern(input.getEpisodeNamePattern())
          .setFolderName(input.getFolderName())
          .setMessageId(Uuid.randomUUID())
          .build();
      return KeyValue.of(key, ShowMsg.newBuilder().setKey(key).setValue(value).build());
    };
  }

  @Bean
  Translator<KeyValue<ShowMsgKey, ShowMsg>, ShowObject> showMsgToLocalTranslator() {
    return pair -> {
      Map<String, String> sources = pair.getValue().getValue().getSources();
      Optional<Entry<String, String>> firstSource = sources.entrySet().stream().findFirst();
      SourceName sourceName = firstSource.map(src -> SourceName.valueOf(src.getKey())).orElse(null);
      String url = firstSource.map(Entry::getValue).orElse(null);
      return ShowObject.builder()
          .title(pair.getValue().getValue().getTitle())
          .season(pair.getValue().getValue().getSeason())
          .showId(pair.getValue().getKey().getShowId())
          .isActive(pair.getValue().getValue().getIsActive())
          .initialSkipEpisodeString(pair.getValue().getValue().getSkipEpisodeString())
          .releaseScheduleCron(pair.getValue().getValue().getReleaseScheduleCron())
          .sourceName(sourceName)
          .url(url)
          .episodeNamePattern(pair.getValue().getValue().getEpisodeNamePattern())
          .folderName(pair.getValue().getValue().getFolderName())
          .build();
    };
  }

}
