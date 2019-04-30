package com.brokencircuits.kissad.restshow.streams;

import com.brokencircuits.kissad.kafka.KeyValueStore;
import com.brokencircuits.kissad.kafka.StreamsService;
import com.brokencircuits.kissad.kafka.Topic;
import com.brokencircuits.kissad.messages.DownloadAvailability;
import com.brokencircuits.kissad.messages.KissShowMessage;
import com.brokencircuits.kissad.restshow.poll.PollNewEpisodeScheduleController;
import java.util.Collections;
import java.util.Map.Entry;
import java.util.Properties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Materialized;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class ShowStreams extends StreamsService {

  private final Properties streamProperties;
  private final Topic<Long, KissShowMessage> showTopic;
  private final Topic<Long, KissShowMessage> showPrivateTopic;
  private final Topic<String, DownloadAvailability> downloadAvailabilityTopic;
  private final KeyValueStore<Long, KissShowMessage> showMessageStore;
  private final Topic<String, Long> urlToShowIdTopic;
  private final KeyValueStore<String, Long> showIdLookupStore;
  private final PollNewEpisodeScheduleController pollNewEpisodeScheduleController;

  @Override
  protected void afterStreamsStart(KafkaStreams streams) {
    showIdLookupStore.initialize(streams);
    showMessageStore.initialize(streams);
  }

  @Override
  protected KafkaStreams getStreams() {
    return new KafkaStreams(buildTopology(), streamProperties);
  }

  private Topology buildTopology() {

    KStream<Long, KissShowMessage> showStream = streamsBuilder
        .stream(showTopic.getName(), showTopic.consumed())
        .peek((id, msg) -> log.info("Show Added: {} : {}", id, msg));

    // write all records from show topic to urlToShowId topic, so we can look up ID by url
    showStream.flatMap((showId, showMsg) -> {
      String url;
      if (showMsg == null) {
        showId = null;
        url = getUrlFromIdLookupStore(showId);
      } else {
        url = showMsg.getUrl();
      }

      if (url != null) {
        return Collections.singleton(new KeyValue<>(url, showId));
      } else {
        return Collections.emptyList();
      }

    })
        .peek((url, id) -> log.info("Adding {} : {} to showId lookup GKT", url, id))
        .to(urlToShowIdTopic.getName(), urlToShowIdTopic.produced());

    // since we already consumed the show topic in stream, we can't do it again for GKT
    // redirect the show topic to new private topic, which will be consumed with GKT
    showStream.to(showPrivateTopic.getName(), showPrivateTopic.produced());

    // listen to downloaderAvailability topic, and notify the scheduleController of changes
    streamsBuilder.stream(downloadAvailabilityTopic.getName(), downloadAvailabilityTopic.consumed())
        .peek((key, msg) -> pollNewEpisodeScheduleController
            .setDownloaderBusy(msg.getAvailableCapacity() == 0));

    // Create GKT with <URL>:<ShowId> mapping
    streamsBuilder.globalTable(urlToShowIdTopic.getName(), urlToShowIdTopic.consumed(),
        Materialized.as(showIdLookupStore.getName()));

    // Create GKT with <ShowId>:<ShowDetails> mapping
    streamsBuilder.globalTable(showPrivateTopic.getName(), showPrivateTopic.consumed(),
        Materialized.as(showMessageStore.getName()));

    return streamsBuilder.build();
  }

  private String getUrlFromIdLookupStore(Long showId) {
    for (Entry<String, Long> entry : showIdLookupStore.asMap().entrySet()) {
      String url = entry.getKey();
      Long id = entry.getValue();
      if (id.equals(showId)) {
        return url;
      }
    }
    return null;
  }
}
