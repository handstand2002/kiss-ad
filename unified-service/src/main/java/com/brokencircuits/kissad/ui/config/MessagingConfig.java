package com.brokencircuits.kissad.ui.config;

import com.brokencircuits.kissad.kafka.ByteKey;
import com.brokencircuits.kissad.kafka.Topic;
import com.brokencircuits.kissad.kafka.config.KafkaConfig;
import com.brokencircuits.kissad.kafka.consumer.ConsumerProvider;
import com.brokencircuits.kissad.kafka.table.KafkaBackedTable;
import com.brokencircuits.kissad.kafka.table.ProducerProvider;
import com.brokencircuits.kissad.kafka.table.StorageProvider;
import com.brokencircuits.kissad.kafka.table.TableBuilder;
import com.brokencircuits.kissad.messages.EpisodeMsg;
import com.brokencircuits.kissad.messages.EpisodeMsgKey;
import com.brokencircuits.kissad.messages.ShowMsg;
import com.brokencircuits.kissad.messages.ShowMsgKey;
import com.brokencircuits.kissad.topics.TopicUtil;
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
  KafkaBackedTable<ByteKey<EpisodeMsgKey>, EpisodeMsg> episodeTable(TableBuilder builder,
      Topic<ByteKey<EpisodeMsgKey>, EpisodeMsg> episodeStoreTopic) {
    return builder.backedTable(episodeStoreTopic);
  }

  @Bean
  KafkaBackedTable<ByteKey<ShowMsgKey>, ShowMsg> showTable(TableBuilder builder,
      Topic<ByteKey<ShowMsgKey>, ShowMsg> showStoreTopic) {
    return builder.backedTable(showStoreTopic);
  }

  @Bean
  CommandLineRunner cleanupRunner(KafkaBackedTable<ByteKey<ShowMsgKey>, ShowMsg> showTable,
      KafkaBackedTable<ByteKey<EpisodeMsgKey>, EpisodeMsg> episodeTable) {
    return args -> {
      log.info("Cleaning up episode table, based on showTable");
      episodeTable.all(kv -> {
        ByteKey<ShowMsgKey> showKey = new ByteKey<>(kv.getValue().getKey().getShowId());
        ShowMsg showMsg = showTable.get(showKey);
        if (showMsg == null) {
          log.info("Stale episode message, cleaning episode {}", kv);
          episodeTable.put(kv.getKey(), null);
        }

      });
    };
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
