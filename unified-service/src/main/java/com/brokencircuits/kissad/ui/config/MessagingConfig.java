package com.brokencircuits.kissad.ui.config;

import com.brokencircuits.download.messages.DownloadType;
import com.brokencircuits.kissad.kafka.ByteKey;
import com.brokencircuits.kissad.kafka.KeyValue;
import com.brokencircuits.kissad.kafka.Topic;
import com.brokencircuits.kissad.kafka.config.KafkaConfig;
import com.brokencircuits.kissad.kafka.consumer.ConsumerProvider;
import com.brokencircuits.kissad.kafka.table.KafkaBackedTable;
import com.brokencircuits.kissad.kafka.table.ProducerProvider;
import com.brokencircuits.kissad.kafka.table.ReadWriteTable;
import com.brokencircuits.kissad.kafka.table.StorageProvider;
import com.brokencircuits.kissad.kafka.table.TableBuilder;
import com.brokencircuits.kissad.messages.EpisodeLink;
import com.brokencircuits.kissad.messages.EpisodeMsg;
import com.brokencircuits.kissad.messages.EpisodeMsgKey;
import com.brokencircuits.kissad.messages.EpisodeMsgValue;
import com.brokencircuits.kissad.messages.ShowMsg;
import com.brokencircuits.kissad.messages.ShowMsgKey;
import com.brokencircuits.kissad.messages.ShowMsgValue;
import com.brokencircuits.kissad.topics.TopicUtil;
import com.brokencircuits.kissad.ui.domain.DownloadTypeDto;
import com.brokencircuits.kissad.ui.domain.EpisodeDto;
import com.brokencircuits.kissad.ui.domain.EpisodeId;
import com.brokencircuits.kissad.ui.domain.EpisodeLinkDto;
import com.brokencircuits.kissad.ui.domain.ShowDto;
import com.brokencircuits.kissad.ui.fetcher.SpFetcher;
import com.brokencircuits.kissad.ui.repository.EpisodeRepository;
import com.brokencircuits.kissad.ui.repository.RepositoryBasedTable;
import com.brokencircuits.kissad.ui.repository.ShowRepository;
import com.brokencircuits.kissad.util.Uuid;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Slf4j
@Configuration
@Import(KafkaConfig.class)
public class MessagingConfig {

  @Bean
  Topic<ByteKey<ShowMsgKey>, ShowMsg> showStoreTopic(
      @Value("${messaging.schema-registry-url}") String schemaRegistryUrl) {
    return TopicUtil.showStoreTopic(schemaRegistryUrl);
  }

  @Bean
  Topic<ByteKey<EpisodeMsgKey>, EpisodeMsg> episodeStoreTopic(
      @Value("${messaging.schema-registry-url}") String schemaRegistryUrl) {
    return TopicUtil.episodeStoreTopic(schemaRegistryUrl);
  }

  @Bean
  ConsumerProvider consumerProvider() {
    return ConsumerProvider.getDefault();
  }

  @Bean
  ProducerProvider producerProvider() {
    return ProducerProvider.getDefault();
  }

  @Bean
  StorageProvider storageProvider() {
    return StorageProvider.hashMapBased();
  }

  @Bean
  ReadWriteTable<ByteKey<EpisodeMsgKey>, EpisodeMsg> episodeTable(
      EpisodeRepository episodeRepository) {

    Function<KeyValue<ByteKey<EpisodeMsgKey>, EpisodeMsg>, EpisodeDto> convertToEntity = kv -> convertEpisode(
        kv.getValue());
    Function<EpisodeDto, KeyValue<ByteKey<EpisodeMsgKey>, EpisodeMsg>> convertToKeyValue = this::convertEpisode;
    Function<ByteKey<EpisodeMsgKey>, EpisodeId> convertKey = b -> new EpisodeId(
        b.getInner().getShowId().getShowId().toString(),
        Math.toIntExact(b.getInner().getEpisodeNumber()));

    return new RepositoryBasedTable<>(episodeRepository, convertToEntity, convertToKeyValue,
        convertKey);
  }

  private KeyValue<ByteKey<EpisodeMsgKey>, EpisodeMsg> convertEpisode(EpisodeDto dto) {
    EpisodeMsgKey msgKey = EpisodeMsgKey.newBuilder()
        .setShowId(ShowMsgKey.newBuilder().setShowId(Uuid.fromString(
            dto.getShowId())).build()).setEpisodeNumber((long) dto.getEpisodeNumber())
        .setRawTitle(null).build();
    ByteKey<EpisodeMsgKey> key = new ByteKey<>(msgKey);

    EpisodeMsg value = EpisodeMsg.newBuilder()
        .setKey(msgKey)
        .setValue(EpisodeMsgValue.newBuilder()
            .setDownloadTime(dto.getDownloadTime())
            .setDownloadedQuality(dto.getDownloadedQuality())
            .setLatestLinks(convertLinksToMsg(dto.getLatestLinks()))
            .setMessageId(Uuid.randomUUID())
            .build())
        .build();
    return KeyValue.of(key, value);
  }

  private List<EpisodeLink> convertLinksToMsg(List<EpisodeLinkDto> latestLinks) {
    List<EpisodeLink> output = new ArrayList<>(latestLinks.size());
    for (EpisodeLinkDto link : latestLinks) {
      output.add(EpisodeLink.newBuilder()
          .setQuality(link.getQuality())
          .setType(DownloadType.valueOf(link.getType().name()))
          .setUrl(link.getUrl())
          .build());
    }
    return output;
  }

  @Bean
  CommandLineRunner migrateShowsToDb(TableBuilder builder,
      Topic<ByteKey<ShowMsgKey>, ShowMsg> showStoreTopic,
      ShowRepository showRepository) {
    return args -> {
      long showCount = showRepository.count();
      if (showCount > 0) {
        log.info("Not migrating shows, as there are already {} shows in database", showCount);
      } else {

        KafkaBackedTable<ByteKey<ShowMsgKey>, ShowMsg> backedTable = builder.backedTable(
            showStoreTopic);
        backedTable.initialize();
        backedTable.all(kv -> showRepository.save(convertShow(kv.getValue())));
        log.info("Migrated {} shows from Kafka to H2", showRepository.count());
      }
    };
  }

  @Bean
  CommandLineRunner migrateEpisodesToDb(TableBuilder builder,
      Topic<ByteKey<EpisodeMsgKey>, EpisodeMsg> episodeStoreTopic,
      EpisodeRepository episodeRepository) {

    return args -> {
      long episodeCount = episodeRepository.count();
      if (episodeCount > 0) {
        log.info("Not migrating episodes, as there are already {} shows in database", episodeCount);
      } else {

        KafkaBackedTable<ByteKey<EpisodeMsgKey>, EpisodeMsg> backedTable = builder.backedTable(
            episodeStoreTopic);
        backedTable.initialize();
        backedTable.all(kv -> {
          EpisodeDto dto = convertEpisode(kv.getValue());
          log.info("Migrating episode to H2: {}", dto);
          episodeRepository.save(dto);
        });
        log.info("Migrated {} episodes from Kafka to H2", episodeRepository.count());
      }
    };
  }

  private EpisodeDto convertEpisode(EpisodeMsg value) {
    return EpisodeDto.builder()
        .showId(value.getKey().getShowId().getShowId().toString())
        .episodeNumber(Math.toIntExact(value.getKey().getEpisodeNumber()))
        .downloadTime(value.getValue().getDownloadTime())
        .downloadedQuality(value.getValue().getDownloadedQuality())
        .latestLinks(convertLinksToDto(value.getValue().getLatestLinks()))
        .build();
  }

  private List<EpisodeLinkDto> convertLinksToDto(List<EpisodeLink> latestLinks) {
    List<EpisodeLinkDto> output = new LinkedList<>();
    for (EpisodeLink link : latestLinks) {
      output.add(
          new EpisodeLinkDto(link.getQuality(), DownloadTypeDto.valueOf(link.getType().name()),
              link.getUrl()));
    }
    return output;
  }

  private ShowDto convertShow(ShowMsg msg) {
    String url = msg.getValue().getSources().get(SpFetcher.SOURCE_IDENTIFIER);
    if (url == null) {
      throw new IllegalStateException(
          "Could not find correct source for show. Values in source map: " + msg.getValue()
              .getSources());
    }
    return ShowDto.builder()
        .id(msg.getKey().getShowId().toString())
        .title(msg.getValue().getTitle())
        .season(msg.getValue().getSeason())
        .releaseScheduleCron(msg.getValue().getReleaseScheduleCron())
        .skipEpisodeString(msg.getValue().getSkipEpisodeString())
        .episodeNamePattern(msg.getValue().getEpisodeNamePattern())
        .folderName(msg.getValue().getFolderName())
        .sourceType(SpFetcher.SOURCE_IDENTIFIER)
        .source(url)
        .build();
  }

  @Bean
  ReadWriteTable<ByteKey<ShowMsgKey>, ShowMsg> showTable(TableBuilder builder,
      Topic<ByteKey<ShowMsgKey>, ShowMsg> showStoreTopic, ShowRepository showRepository) {

    Function<KeyValue<ByteKey<ShowMsgKey>, ShowMsg>, ShowDto> convertToEntity = kv -> convertShow(
        kv.getValue());
    Function<ShowDto, KeyValue<ByteKey<ShowMsgKey>, ShowMsg>> convertToKv = dto -> {
      ShowMsg showMsg = convertShow(dto);
      return KeyValue.of(new ByteKey<>(showMsg.getKey()), showMsg);
    };
    Function<ByteKey<ShowMsgKey>, String> convertKey = k -> k.getInner().getShowId().toString();

    return new RepositoryBasedTable<>(showRepository, convertToEntity, convertToKv, convertKey);
  }

  private ShowMsg convertShow(ShowDto dto) {
    ShowMsgKey key = ShowMsgKey.newBuilder()
        .setShowId(Uuid.fromString(dto.getId()))
        .build();
    Map<String, String> sources = new HashMap<>();
    sources.put(dto.getSourceType(), dto.getSource());
    ShowMsgValue value = ShowMsgValue.newBuilder()
        .setMessageId(Uuid.randomUUID())
        .setTitle(dto.getTitle())
        .setSeason(dto.getSeason())
        .setSources(sources)
        .setIsActive(true)
        .setReleaseScheduleCron(dto.getReleaseScheduleCron())
        .setSkipEpisodeString(dto.getSkipEpisodeString())
        .setEpisodeNamePattern(dto.getEpisodeNamePattern())
        .setFolderName(dto.getFolderName())
        .build();
    return ShowMsg.newBuilder()
        .setKey(key)
        .setValue(value)
        .build();
  }

  @Bean
  TableBuilder tableBuilder(ConsumerProvider consumerProvider, ProducerProvider producerProvider,
      KafkaConfig kafkaConfig, StorageProvider storageProvider) {
    return TableBuilder.builder()
        .consumerProvider(consumerProvider)
        .producerProvider(producerProvider)
        .storageProvider(storageProvider)
        .config(kafkaConfig)
        .build();
  }

}
