package com.brokencircuits.kissad.streamrvfetch.streams;

import com.brokencircuits.kissad.kafka.StreamsService;
import com.brokencircuits.kissad.kafka.Topic;
import com.brokencircuits.kissad.messages.ExternalEpisodeLinkMessage;
import com.brokencircuits.kissad.messages.KissEpisodePageKey;
import com.brokencircuits.kissad.messages.VideoSource;
import com.brokencircuits.kissad.streamrvfetch.streamprocessing.ExtLinkMessageProcessor;
import java.util.Properties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.Topology;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExtLinkStreams extends StreamsService {

  private final Properties streamProperties;
  private final Topic<ByteKey<KissEpisodePageKey>, ExternalEpisodeLinkMessage> externalLinkTopic;
  private final ExtLinkMessageProcessor extLinkMessageProcessor;

  @Override
  protected KafkaStreams getStreams() {
    return new KafkaStreams(buildTopology(), streamProperties);
  }

  private Topology buildTopology() {
    streamsBuilder.stream(externalLinkTopic.getName(), externalLinkTopic.consumedWith())
        .filter((key, msg) -> msg.getVideoSource().equals(VideoSource.RAPIDVIDEO))
        .process(() -> extLinkMessageProcessor);

    return streamsBuilder.build();
  }
}