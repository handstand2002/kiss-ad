package com.brokencircuits.kissad.topics;

import com.brokencircuits.downloader.messages.DownloadRequestKey;
import com.brokencircuits.downloader.messages.DownloadRequestValue;
import com.brokencircuits.downloader.messages.DownloadStatusKey;
import com.brokencircuits.downloader.messages.DownloadStatusValue;
import com.brokencircuits.kissad.kafka.Topic;
import com.brokencircuits.kissad.kafka.Util;
import com.brokencircuits.kissad.messages.Episode;
import com.brokencircuits.kissad.messages.EpisodeKey;
import com.brokencircuits.kissad.messages.ShowMsgKey;
import com.brokencircuits.kissad.messages.ShowMsgValue;
import java.util.HashMap;
import java.util.Map;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.common.serialization.Serde;

public class TopicUtil {

  private static Serde<SpecificRecord> keySerde = null;
  private static Serde<SpecificRecord> valueSerde = null;
  private static Map<String, Topic> topicCache = new HashMap<>();

  public static final String TOPIC_SHOW_STORE = "ad.show.store";
  public static final String TOPIC_SHOW_QUEUE = "ad.show.queue";
  public static final String TOPIC_EPISODE_STORE = "ad.episode.store";
  public static final String TOPIC_EPISODE_QUEUE = "ad.episode.queue";
  public static final String TOPIC_DOWNLOAD_COMMAND = "download.command";
  public static final String TOPIC_DOWNLOAD_STATUS = "download.status";

  /**
   * Topic containing interest list of shows
   */
  @SuppressWarnings("unchecked")
  public static Topic<ShowMsgKey, ShowMsgValue> showStoreTopic(String schemaRegistryUrl) {
    return getTopic(TOPIC_SHOW_STORE, schemaRegistryUrl);
  }

  /**
   * Trigger topic for shows
   */
  @SuppressWarnings("unchecked")
  public static Topic<ShowMsgKey, ShowMsgValue> showQueueTopic(String schemaRegistryUrl) {
    return getTopic(TOPIC_SHOW_QUEUE, schemaRegistryUrl);
  }

  @SuppressWarnings("unchecked")
  public static Topic<EpisodeKey, Episode> episodeStoreTopic(String schemaRegistryUrl) {
    return getTopic(TOPIC_EPISODE_STORE, schemaRegistryUrl);
  }

  @SuppressWarnings("unchecked")
  public static Topic<EpisodeKey, Episode> episodeQueueTopic(String schemaRegistryUrl) {
    return getTopic(TOPIC_EPISODE_QUEUE, schemaRegistryUrl);
  }

  @SuppressWarnings("unchecked")
  public static Topic<DownloadRequestKey, DownloadRequestValue> downloadCommandTopic(
      String schemaRegistryUrl) {
    return getTopic(TOPIC_DOWNLOAD_COMMAND, schemaRegistryUrl);
  }

  @SuppressWarnings("unchecked")
  public static Topic<DownloadStatusKey, DownloadStatusValue> downloadStatusTopic(
      String schemaRegistryUrl) {
    return getTopic(TOPIC_DOWNLOAD_STATUS, schemaRegistryUrl);
  }

  @SuppressWarnings("unchecked")
  private static Topic getTopic(String topicName, String schemaRegistryUrl) {
    if (!topicCache.containsKey(topicName)) {
      topicCache.put(topicName,
          new Topic<>(topicName, getKeySerde(schemaRegistryUrl), getValueSerde(schemaRegistryUrl)));
    }
    return topicCache.get(topicName);
  }

  public static <T> Serde<T> getKeySerde(String schemaRegistryUrl) {
    if (keySerde == null) {
      keySerde = Util.createAvroSerde(schemaRegistryUrl, true);
    }
    return (Serde<T>) keySerde;
  }

  public static <T> Serde<T> getValueSerde(String schemaRegistryUrl) {
    if (valueSerde == null) {
      valueSerde = Util.createAvroSerde(schemaRegistryUrl, false);
    }
    return (Serde<T>) valueSerde;
  }
}