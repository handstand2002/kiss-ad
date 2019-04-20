package com.brokencircuits.kissad.streamepisodelink.streams;

import com.brokencircuits.kissad.kafka.StreamsService;
import com.brokencircuits.kissad.kafka.Topic;
import com.brokencircuits.kissad.messages.KissEpisodePageKey;
import com.brokencircuits.kissad.messages.KissEpisodePageMessage;
import com.brokencircuits.kissad.streamepisodelink.streamprocessing.EpisodeMessageProcessor;
import java.util.Properties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.Topology;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EpisodeStreams extends StreamsService {

  private final Properties streamProperties;
  private final Topic<KissEpisodePageKey, KissEpisodePageMessage> episodeTopic;
  private final EpisodeMessageProcessor showMessageProcessor;

  @Override
  protected KafkaStreams getStreams() {
    return new KafkaStreams(buildTopology(), streamProperties);
  }

  private Topology buildTopology() {
    streamsBuilder.stream(episodeTopic.getName(), episodeTopic.consumed())
        .process(() -> showMessageProcessor);

    return streamsBuilder.build();
  }
}
