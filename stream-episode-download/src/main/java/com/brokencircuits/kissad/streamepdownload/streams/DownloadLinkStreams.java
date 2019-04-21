package com.brokencircuits.kissad.streamepdownload.streams;


import com.brokencircuits.kissad.kafka.StreamsService;
import com.brokencircuits.kissad.kafka.Topic;
import com.brokencircuits.kissad.messages.ExternalDownloadLinkKey;
import com.brokencircuits.kissad.messages.ExternalDownloadLinkMessage;
import com.brokencircuits.kissad.messages.ExternalEpisodeLinkMessage;
import com.brokencircuits.kissad.messages.KissEpisodePageKey;
import com.brokencircuits.kissad.messages.VideoSource;
import com.brokencircuits.kissad.streamepdownload.streamprocessing.DownloadLinkProcessor;
import java.util.Properties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.Topology;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DownloadLinkStreams extends StreamsService {

  private final Properties streamProperties;
  private final Topic<ExternalDownloadLinkKey, ExternalDownloadLinkMessage> downloadLinkTopic;
  private final DownloadLinkProcessor downloadLinkProcessor;

  @Override
  protected KafkaStreams getStreams() {
    return new KafkaStreams(buildTopology(), streamProperties);
  }

  private Topology buildTopology() {
    streamsBuilder.stream(downloadLinkTopic.getName(), downloadLinkTopic.consumed())
        .process(() -> downloadLinkProcessor);

    return streamsBuilder.build();
  }
}
