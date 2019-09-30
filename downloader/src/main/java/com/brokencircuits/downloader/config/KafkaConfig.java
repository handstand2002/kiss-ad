package com.brokencircuits.downloader.config;

import static com.brokencircuits.kissad.kafka.Util.createAvroSerde;

import com.brokencircuits.downloader.messages.DownloadRequestKey;
import com.brokencircuits.downloader.messages.DownloadRequestValue;
import com.brokencircuits.downloader.messages.DownloadStatusKey;
import com.brokencircuits.downloader.messages.DownloadStatusValue;
import com.brokencircuits.downloader.messages.DownloaderStatusKey;
import com.brokencircuits.downloader.messages.DownloaderStatusValue;
import com.brokencircuits.kissad.kafka.Topic;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.serialization.Serde;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class KafkaConfig {

  @Bean
  Topic<DownloadRequestKey, DownloadRequestValue> downloadRequestTopic(
      @Value("download-instruction") String topicName, Serde<DownloadRequestKey> keySerde,
      Serde<DownloadRequestValue> valueSerde) {
    return new Topic<>(topicName, keySerde, valueSerde);
  }

  @Bean
  public NewTopic createDownloadRequestTopic(
      Topic<DownloadRequestKey, DownloadRequestValue> topic) {
    return new NewTopic(topic.getName(), 1, (short) 1);
  }

  @Bean
  Serde<DownloadRequestKey> downloadRequestKeySerde(
      @Value("${messaging.schema-registry-url}") String schemaRegistryUrl) {
    return createAvroSerde(schemaRegistryUrl, true);
  }

  @Bean
  Serde<DownloadRequestValue> downloadRequestValueSerde(
      @Value("${messaging.schema-registry-url}") String schemaRegistryUrl) {
    return createAvroSerde(schemaRegistryUrl, false);
  }


  @Bean
  Topic<DownloadStatusKey, DownloadStatusValue> downloadStatusTopic(
      @Value("download-status") String topicName,
      Serde<DownloadStatusKey> keySerde, Serde<DownloadStatusValue> valueSerde) {
    return new Topic<>(topicName, keySerde, valueSerde);
  }

  @Bean
  public NewTopic createDownloadStatusTopic(Topic<DownloadStatusKey, DownloadStatusValue> topic) {
    return new NewTopic(topic.getName(), 1, (short) 1);
  }

  @Bean
  Serde<DownloadStatusKey> downloadStatusKeySerde(
      @Value("${messaging.schema-registry-url}") String schemaRegistryUrl) {
    return createAvroSerde(schemaRegistryUrl, true);
  }

  @Bean
  Serde<DownloadStatusValue> downloadStatusValueSerde(
      @Value("${messaging.schema-registry-url}") String schemaRegistryUrl) {
    return createAvroSerde(schemaRegistryUrl, false);
  }


  @Bean
  Topic<DownloaderStatusKey, DownloaderStatusValue> downloaderStatusTopic(
      @Value("downloader-status") String topicName, Serde<DownloaderStatusKey> keySerde,
      Serde<DownloaderStatusValue> valueSerde) {
    return new Topic<>(topicName, keySerde, valueSerde);
  }

  @Bean
  public NewTopic createDownloaderStatusTopic(
      Topic<DownloaderStatusKey, DownloaderStatusValue> topic) {
    return new NewTopic(topic.getName(), 1, (short) 1);
  }

  @Bean
  Serde<DownloaderStatusKey> downloaderStatusKeySerde(
      @Value("${messaging.schema-registry-url}") String schemaRegistryUrl) {
    return createAvroSerde(schemaRegistryUrl, true);
  }

  @Bean
  Serde<DownloaderStatusValue> downloaderStatusValueSerde(
      @Value("${messaging.schema-registry-url}") String schemaRegistryUrl) {
    return createAvroSerde(schemaRegistryUrl, false);
  }


}
