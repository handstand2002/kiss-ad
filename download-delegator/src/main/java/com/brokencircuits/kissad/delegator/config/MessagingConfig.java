package com.brokencircuits.kissad.delegator.config;

import com.brokencircuits.downloader.messages.DownloadRequestKey;
import com.brokencircuits.downloader.messages.DownloadRequestMsg;
import com.brokencircuits.downloader.messages.DownloadStatusKey;
import com.brokencircuits.downloader.messages.DownloadStatusMsg;
import com.brokencircuits.kissad.kafka.AdminInterface;
import com.brokencircuits.kissad.kafka.ByteKey;
import com.brokencircuits.kissad.kafka.Publisher;
import com.brokencircuits.kissad.kafka.StateStoreDetails;
import com.brokencircuits.kissad.kafka.Topic;
import com.brokencircuits.kissad.kafka.config.KafkaConfig;
import com.brokencircuits.kissad.messages.EpisodeMsg;
import com.brokencircuits.kissad.messages.EpisodeMsgKey;
import com.brokencircuits.kissad.messages.EpisodeMsgValue;
import com.brokencircuits.kissad.messages.ShowMsg;
import com.brokencircuits.kissad.messages.ShowMsgKey;
import com.brokencircuits.kissad.topics.TopicUtil;
import com.brokencircuits.kissad.util.Uuid;
import com.brokencircuits.messages.Command;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.KeyValue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Slf4j
@Configuration
@Import(KafkaConfig.class)
public class MessagingConfig {

  private static final String NOT_DOWNLOADED_EPISODE_STORE_NAME = "not-downloaded-episodes";
  private static final String STORE_SHOW = "shows";
  private static final String STORE_EPISODE = "episodes";

  @Bean
  StateStoreDetails<ByteKey<EpisodeMsgKey>, EpisodeMsg> notDownloadedStoreDetails(
      Topic<ByteKey<EpisodeMsgKey>, EpisodeMsg> episodeQueueTopic,
      @Value("${messaging.schema-registry-url}") String schemaRegistryUrl) {
    return new StateStoreDetails<>(NOT_DOWNLOADED_EPISODE_STORE_NAME,
        episodeQueueTopic.getKeySerde(), episodeQueueTopic.getValueSerde());
  }

  @Bean
  Topic<ByteKey<EpisodeMsgKey>, EpisodeMsg> episodeQueueTopic(
      @Value("${messaging.schema-registry-url}") String schemaRegistryUrl) {
    return TopicUtil.episodeQueueTopic(schemaRegistryUrl);
  }

  @Bean
  Topic<ByteKey<EpisodeMsgKey>, EpisodeMsg> episodeStoreTopic(
      @Value("${messaging.schema-registry-url}") String schemaRegistryUrl) {
    return TopicUtil.episodeStoreTopic(schemaRegistryUrl);
  }

  @Bean
  Topic<ByteKey<DownloadRequestKey>, DownloadRequestMsg> downloadRequestTopic(
      @Value("${messaging.schema-registry-url}") String schemaRegistryUrl) {
    return TopicUtil.downloadRequestTopic(schemaRegistryUrl);
  }

  @Bean
  Topic<ByteKey<DownloadStatusKey>, DownloadStatusMsg> downloadStatusTopic(
      @Value("${messaging.schema-registry-url}") String schemaRegistryUrl) {
    return TopicUtil.downloadStatusTopic(schemaRegistryUrl);
  }

  @Bean
  Topic<ByteKey<ShowMsgKey>, ShowMsg> showStoreTopic(
      @Value("${messaging.schema-registry-url}") String schemaRegistryUrl) {
    return TopicUtil.showStoreTopic(schemaRegistryUrl);
  }

  @Bean
  StateStoreDetails<ByteKey<ShowMsgKey>, ShowMsg> showStoreDetails(
      @Value("${messaging.schema-registry-url}") String schemaRegistryUrl,
      Topic<ByteKey<ShowMsgKey>, ShowMsg> showStoreTopic) {
    return new StateStoreDetails<>(STORE_SHOW, showStoreTopic.getKeySerde(),
        showStoreTopic.getValueSerde());
  }

  @Bean
  StateStoreDetails<ByteKey<EpisodeMsgKey>, EpisodeMsg> episodeStoreDetails(
      Topic<ByteKey<EpisodeMsgKey>, EpisodeMsg> episodeStoreTopic,
      @Value("${messaging.schema-registry-url}") String schemaRegistryUrl) {
    return new StateStoreDetails<>(STORE_EPISODE, episodeStoreTopic.getKeySerde(),
        episodeStoreTopic.getValueSerde());
  }

  @Bean
  AdminInterface adminInterface(@Value("${messaging.schema-registry-url}") String schemaRegistryUrl,
                                KafkaConfig props,
                                Publisher<ByteKey<EpisodeMsgKey>, EpisodeMsg> episodeStorePublisher) throws Exception {
    AdminInterface adminInterface = new AdminInterface(props, schemaRegistryUrl);
    adminInterface.registerCommand(Command.SKIP_EPISODE_RANGE, command -> {

      List<String> parameters = command.getValue().getParameters();

      Uuid showUuid = Uuid.fromString(parameters.get(0));
      Set<Long> episodesToPublish = episodesFromRangeCsv(parameters.get(1));

      episodesToPublish.forEach(episodeNum -> {
        KeyValue<ByteKey<EpisodeMsgKey>, EpisodeMsg> kvPair = downloadedEpisodeMsg(showUuid,
            episodeNum);
        episodeStorePublisher.send(kvPair);
      });
    });
    adminInterface.start();
    return adminInterface;
  }

  private Set<Long> episodesFromRangeCsv(String rangeCsv) {
    String[] ranges = rangeCsv.split(" *, *");
    HashSet<Long> episodesToPublish = new HashSet<>();
    Pattern rangePattern = Pattern.compile("\\s*(\\d+)\\s*(-\\s*(\\d+)\\s*)?");
    for (String range : ranges) {
      Matcher matcher = rangePattern.matcher(range);
      if (matcher.find()) {
        int startRange = Integer.parseInt(matcher.group(1));
        int endRange = startRange;
        String endRangeString = matcher.group(3);
        if (endRangeString != null) {
          endRange = Integer.parseInt(endRangeString);
        }
        for (long i = startRange; i <= endRange; i++) {
          episodesToPublish.add(i);
        }
      }
    }
    return episodesToPublish;
  }

  private KeyValue<ByteKey<EpisodeMsgKey>, EpisodeMsg> downloadedEpisodeMsg(Uuid showUuid,
                                                                            Long episodeNum) {
    EpisodeMsgKey key = EpisodeMsgKey.newBuilder()
        .setEpisodeNumber(episodeNum)
        .setShowId(ShowMsgKey.newBuilder().setShowId(showUuid).build())
        .build();
    EpisodeMsgValue value = EpisodeMsgValue.newBuilder()
        .setMessageId(Uuid.randomUUID())
        .setLatestLinks(new ArrayList<>())
        .setDownloadTime(Instant.now())
        .setDownloadedQuality(-1)
        .build();
    EpisodeMsg msg = EpisodeMsg.newBuilder()
        .setKey(key)
        .setValue(value)
        .build();
    return KeyValue.pair(ByteKey.from(key), msg);
  }

}
