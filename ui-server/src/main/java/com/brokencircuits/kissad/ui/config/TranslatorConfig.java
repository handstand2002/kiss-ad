package com.brokencircuits.kissad.ui.config;

import com.brokencircuits.kissad.Translator;
import com.brokencircuits.kissad.kafka.ByteKey;
import com.brokencircuits.kissad.messages.ShowMsg;
import com.brokencircuits.kissad.messages.ShowMsgKey;
import com.brokencircuits.kissad.messages.ShowMsgValue;
import com.brokencircuits.kissad.messages.SourceName;
import com.brokencircuits.kissad.ui.rest.domain.HsShowObject;
import com.brokencircuits.kissad.ui.rest.domain.ShowObject;
import com.brokencircuits.kissad.ui.rest.domain.ShowSource;
import com.brokencircuits.kissad.util.Uuid;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
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
          .setSources(convertSources(input.getSourceName(), input.getUrl()))
          .setIsActive(input.getIsActive())
          .setReleaseScheduleCron(input.getReleaseScheduleCron())
          .setSkipEpisodeString(input.getInitialSkipEpisodeString())
          .setEpisodeNamePattern(input.getEpisodeNamePattern())
          .setFolderName(input.getFolderName())
          .setMessageId(Uuid.randomUUID())
          .build();
      return new KeyValue<>(new ByteKey<>(key),
          ShowMsg.newBuilder().setKey(key).setValue(value).build());
    };
  }

  private Map<String, String> convertSources(SourceName sourceName, String url) {
    Map<String, String> outputMap = new HashMap<>();
    outputMap.put(sourceName.name(), url);
    return outputMap;
  }

  @Bean
  Translator<HsShowObject, KeyValue<ByteKey<ShowMsgKey>, ShowMsg>> hsShowLocalToMsgTranslator() {
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
      return new KeyValue<>(new ByteKey<>(key),
          ShowMsg.newBuilder().setKey(key).setValue(value).build());
    };
  }

  @Bean
  Translator<KeyValue<ByteKey<ShowMsgKey>, ShowMsg>, ShowObject> showMsgToLocalTranslator() {
    return pair -> {
      Map<String, String> sources = pair.value.getValue().getSources();
      Optional<Entry<String, String>> firstSource = sources.entrySet().stream().findFirst();
      SourceName sourceName = firstSource.map(src -> SourceName.valueOf(src.getKey())).orElse(null);
      String url = firstSource.map(Entry::getValue).orElse(null);
      return ShowObject.builder()
          .title(pair.value.getValue().getTitle())
          .season(pair.value.getValue().getSeason())
          .showId(pair.value.getKey().getShowId())
          .isActive(pair.value.getValue().getIsActive())
          .initialSkipEpisodeString(pair.value.getValue().getSkipEpisodeString())
          .releaseScheduleCron(pair.value.getValue().getReleaseScheduleCron())
          .sourceName(sourceName)
          .url(url)
          .episodeNamePattern(pair.value.getValue().getEpisodeNamePattern())
          .folderName(pair.value.getValue().getFolderName())
          .build();
    };
  }

  private static Collection<ShowSource> convertSources(Map<String, String> sources) {
    Collection<ShowSource> outputCollection = Sets.newHashSet();
    sources.forEach((sourceName, url) -> outputCollection
        .add(ShowSource.builder().sourceName(SourceName.valueOf(sourceName)).url(url).build()));

    return outputCollection;
  }


}
