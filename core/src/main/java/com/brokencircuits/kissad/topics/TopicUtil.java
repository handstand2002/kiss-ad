package com.brokencircuits.kissad.topics;

import com.brokencircuits.downloader.messages.DownloadRequestKey;
import com.brokencircuits.downloader.messages.DownloadRequestMsg;
import com.brokencircuits.downloader.messages.DownloadStatusKey;
import com.brokencircuits.downloader.messages.DownloadStatusMsg;
import com.brokencircuits.downloader.messages.DownloaderStatusKey;
import com.brokencircuits.downloader.messages.DownloaderStatusMsg;
import com.brokencircuits.kissad.kafka.ByteKey;
import com.brokencircuits.kissad.kafka.ByteKeySerde;
import com.brokencircuits.kissad.kafka.Topic;
import com.brokencircuits.kissad.kafka.Util;
import com.brokencircuits.kissad.messages.EpisodeMsg;
import com.brokencircuits.kissad.messages.EpisodeMsgKey;
import com.brokencircuits.kissad.messages.ShowMsg;
import com.brokencircuits.kissad.messages.ShowMsgKey;
import com.brokencircuits.messages.AdminCommandKey;
import com.brokencircuits.messages.AdminCommandMsg;
import com.brokencircuits.messages.KissCaptchaImgKey;
import com.brokencircuits.messages.KissCaptchaImgMsg;
import java.util.HashMap;
import java.util.Map;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.common.serialization.Serde;

public class TopicUtil {

  private static Serde<SpecificRecord> keySerde = null;
  private static Serde<SpecificRecord> valueSerde = null;
  private static Map<String, Topic> topicCache = new HashMap<>();

  public static final String TOPIC_KISS_CAPTCHA_PICTURE = "ad.kiss.captcha.picture.store";
  public static final String TOPIC_DOWNLOADER_STATUS = "service.downloader.status.v2";
  public static final String TOPIC_SHOW_STORE = "ad.show.store.v2";
  public static final String TOPIC_SHOW_QUEUE = "ad.show.queue.v2";
  public static final String TOPIC_EPISODE_STORE = "ad.episode.store.v2";
  public static final String TOPIC_EPISODE_QUEUE = "ad.episode.queue.v2";
  public static final String TOPIC_DOWNLOAD_COMMAND = "download.command.v2";
  public static final String TOPIC_DOWNLOAD_STATUS = "download.status.v2";
  public static final String TOPIC_ADMIN = "admin.command.v2";

  public static final String MODULE_API = "ad.showApi.v2";
  public static final String MODULE_SCHEDULER = "ad.scheduler.v2";
  public static final String MODULE_FETCHER_HS = "ad.showFetch.hs.v2";
  public static final String MODULE_DOWNLOAD_DELEGATOR = "ad.downloadDelegator.v2";
  public static final String MODULE_DOWNLOADER = "downloader.v2";

  /**
   * Topic containing interest list of shows
   */
  @SuppressWarnings("unchecked")
  public static Topic<ByteKey<AdminCommandKey>, AdminCommandMsg> adminTopic(
      String schemaRegistryUrl) {
    return getTopic(TOPIC_ADMIN, schemaRegistryUrl);
  }

  /**
   * Topic containing interest list of shows
   */
  @SuppressWarnings("unchecked")
  public static Topic<ByteKey<ShowMsgKey>, ShowMsg> showStoreTopic(String schemaRegistryUrl) {
    return getTopic(TOPIC_SHOW_STORE, schemaRegistryUrl);
  }

  /**
   * Trigger topic for shows
   */
  @SuppressWarnings("unchecked")
  public static Topic<ByteKey<ShowMsgKey>, ShowMsg> showQueueTopic(String schemaRegistryUrl) {
    return getTopic(TOPIC_SHOW_QUEUE, schemaRegistryUrl);
  }

  @SuppressWarnings("unchecked")
  public static Topic<ByteKey<EpisodeMsgKey>, EpisodeMsg> episodeStoreTopic(
      String schemaRegistryUrl) {
    return getTopic(TOPIC_EPISODE_STORE, schemaRegistryUrl);
  }

  @SuppressWarnings("unchecked")
  public static Topic<ByteKey<EpisodeMsgKey>, EpisodeMsg> episodeQueueTopic(
      String schemaRegistryUrl) {
    return getTopic(TOPIC_EPISODE_QUEUE, schemaRegistryUrl);
  }

  @SuppressWarnings("unchecked")
  public static Topic<ByteKey<KissCaptchaImgKey>, KissCaptchaImgMsg> kissCaptchaPictureTopic(
      String schemaRegistryUrl) {
    return getTopic(TOPIC_KISS_CAPTCHA_PICTURE, schemaRegistryUrl);
  }

  @SuppressWarnings("unchecked")
  public static Topic<ByteKey<DownloadRequestKey>, DownloadRequestMsg> downloadRequestTopic(
      String schemaRegistryUrl) {
    return getTopic(TOPIC_DOWNLOAD_COMMAND, schemaRegistryUrl);
  }

  @SuppressWarnings("unchecked")
  public static Topic<ByteKey<DownloadStatusKey>, DownloadStatusMsg> downloadStatusTopic(
      String schemaRegistryUrl) {
    return getTopic(TOPIC_DOWNLOAD_STATUS, schemaRegistryUrl);
  }

  @SuppressWarnings("unchecked")
  public static Topic<ByteKey<DownloaderStatusKey>, DownloaderStatusMsg> downloaderStatusTopic(
      String schemaRegistryUrl) {
    return getTopic(TOPIC_DOWNLOADER_STATUS, schemaRegistryUrl);
  }

  @SuppressWarnings("unchecked")
  private static Topic getTopic(String topicName, String schemaRegistryUrl) {
    if (!topicCache.containsKey(topicName)) {
      topicCache.put(topicName,
          new Topic<>(topicName, new ByteKeySerde<>(), getValueSerde(schemaRegistryUrl)));
    }
    return topicCache.get(topicName);
  }

  public static <T> Serde<T> getValueSerde(String schemaRegistryUrl) {
    if (valueSerde == null) {
      valueSerde = Util.createAvroSerde(schemaRegistryUrl, false);
    }
    return (Serde<T>) valueSerde;
  }
}
