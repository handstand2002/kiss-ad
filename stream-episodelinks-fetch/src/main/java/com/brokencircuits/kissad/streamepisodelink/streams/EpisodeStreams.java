package com.brokencircuits.kissad.streamepisodelink.streams;

import com.brokencircuits.kissad.kafka.StreamsService;
import com.brokencircuits.kissad.kafka.Topic;
import com.brokencircuits.kissad.messages.DownloadAvailability;
import com.brokencircuits.kissad.messages.KissEpisodePageKey;
import com.brokencircuits.kissad.messages.KissEpisodePageMessage;
import com.brokencircuits.kissad.streamepisodelink.streamprocessing.EpisodeMessageProcessor;
import java.util.Properties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Materialized;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EpisodeStreams extends StreamsService {

  private final Properties streamProperties;
  private final Topic<KissEpisodePageKey, KissEpisodePageMessage> episodeTopic;
  private final Topic<String, DownloadAvailability> downloadAvailabilityTopic;
  private final EpisodeMessageProcessor showMessageProcessor;

  @Value("${messaging.stores.download-availability}")
  private String downloadAvailabilityStoreName;

  @Override
  protected KafkaStreams getStreams() {
    return new KafkaStreams(buildTopology(), streamProperties);
  }

  private Topology buildTopology() {
    streamsBuilder.stream(episodeTopic.getName(), episodeTopic.consumedWith())
        .process(() -> showMessageProcessor);
    streamsBuilder
        .globalTable(downloadAvailabilityTopic.getName(), downloadAvailabilityTopic.consumedWith(),
            Materialized.as(downloadAvailabilityStoreName));

    return streamsBuilder.build();
  }
}
