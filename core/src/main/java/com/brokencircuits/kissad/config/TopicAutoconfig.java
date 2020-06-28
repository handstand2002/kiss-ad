package com.brokencircuits.kissad.config;

import com.brokencircuits.downloader.messages.DownloadRequestKey;
import com.brokencircuits.downloader.messages.DownloadRequestMsg;
import com.brokencircuits.downloader.messages.DownloadStatusKey;
import com.brokencircuits.downloader.messages.DownloadStatusMsg;
import com.brokencircuits.downloader.messages.DownloaderStatusKey;
import com.brokencircuits.downloader.messages.DownloaderStatusMsg;
import com.brokencircuits.kissad.kafka.ByteKey;
import com.brokencircuits.kissad.kafka.Topic;
import com.brokencircuits.kissad.messages.EpisodeMsg;
import com.brokencircuits.kissad.messages.EpisodeMsgKey;
import com.brokencircuits.kissad.messages.KissEpisodeExternalSrcMsg;
import com.brokencircuits.kissad.messages.KissEpisodePageKey;
import com.brokencircuits.kissad.messages.KissEpisodePageMessage;
import com.brokencircuits.kissad.messages.ShowMsg;
import com.brokencircuits.kissad.messages.ShowMsgKey;
import com.brokencircuits.kissad.topics.TopicUtil;
import com.brokencircuits.messages.AdminCommandKey;
import com.brokencircuits.messages.AdminCommandMsg;
import com.brokencircuits.messages.KissCaptchaBatchKey;
import com.brokencircuits.messages.KissCaptchaBatchKeywordKey;
import com.brokencircuits.messages.KissCaptchaBatchKeywordMsg;
import com.brokencircuits.messages.KissCaptchaBatchMsg;
import com.brokencircuits.messages.KissCaptchaImgKey;
import com.brokencircuits.messages.KissCaptchaImgMsg;
import com.brokencircuits.messages.KissCaptchaMatchedKeywordMsg;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(value = "messaging.schema-registry-url")
public class TopicAutoconfig {

  @Value("${messaging.schema-registry-url}")
  private String schemaRegistryUrl;

  @Bean
  Topic<ByteKey<EpisodeMsgKey>, KissEpisodeExternalSrcMsg> kissExternalSourceTopic() {
    return TopicUtil.kissExternalSourceTopic(schemaRegistryUrl);
  }

  @Bean
  Topic<ByteKey<AdminCommandKey>, AdminCommandMsg> adminTopic() {
    return TopicUtil.adminTopic(schemaRegistryUrl);
  }

  @Bean
  Topic<ByteKey<KissEpisodePageKey>, KissEpisodePageMessage> kissEpisodePageQueueTopic() {
    return TopicUtil.kissEpisodePageQueueTopic(schemaRegistryUrl);
  }

  @Bean
  Topic<ByteKey<ShowMsgKey>, ShowMsg> showStoreTopic() {
    return TopicUtil.showStoreTopic(schemaRegistryUrl);
  }

  @Bean
  Topic<ByteKey<ShowMsgKey>, ShowMsg> showQueueTopic() {
    return TopicUtil.showQueueTopic(schemaRegistryUrl);
  }

  @Bean
  Topic<ByteKey<EpisodeMsgKey>, EpisodeMsg> episodeStoreTopic() {
    return TopicUtil.episodeStoreTopic(schemaRegistryUrl);
  }

  @Bean
  Topic<ByteKey<EpisodeMsgKey>, EpisodeMsg> episodeQueueTopic() {
    return TopicUtil.episodeQueueTopic(schemaRegistryUrl);
  }

  @Bean
  Topic<ByteKey<KissCaptchaBatchKeywordKey>, KissCaptchaBatchKeywordMsg> kissCaptchaKeywordTopic() {
    return TopicUtil.kissCaptchaKeywordTopic(schemaRegistryUrl);
  }

  @Bean
  Topic<ByteKey<KissCaptchaBatchKeywordKey>, KissCaptchaMatchedKeywordMsg> kissCaptchaMatchedKeywordTopic() {
    return TopicUtil.kissCaptchaMatchedKeywordTopic(schemaRegistryUrl);
  }

  @Bean
  Topic<ByteKey<KissCaptchaImgKey>, KissCaptchaImgMsg> kissCaptchaPictureTopic() {
    return TopicUtil.kissCaptchaPictureTopic(schemaRegistryUrl);
  }

  @Bean
  Topic<ByteKey<KissCaptchaBatchKey>, KissCaptchaBatchMsg> kissCaptchaBatchTopic() {
    return TopicUtil.kissCaptchaBatchTopic(schemaRegistryUrl);
  }

  @Bean
  Topic<ByteKey<DownloadRequestKey>, DownloadRequestMsg> downloadRequestTopic() {
    return TopicUtil.downloadRequestTopic(schemaRegistryUrl);
  }

  @Bean
  Topic<ByteKey<DownloadStatusKey>, DownloadStatusMsg> downloadStatusTopic() {
    return TopicUtil.downloadStatusTopic(schemaRegistryUrl);
  }

  @Bean
  Topic<ByteKey<DownloaderStatusKey>, DownloaderStatusMsg> downloaderStatusTopic() {
    return TopicUtil.downloaderStatusTopic(schemaRegistryUrl);
  }
}
