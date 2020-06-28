package com.brokencircuits.kissad.novaepisodefetch.streams;

import com.brokencircuits.kissad.kafka.ByteKey;
import com.brokencircuits.kissad.kafka.StreamsService;
import com.brokencircuits.kissad.kafka.Topic;
import com.brokencircuits.kissad.messages.EpisodeMsgKey;
import com.brokencircuits.kissad.messages.ExternalSources;
import com.brokencircuits.kissad.messages.KissEpisodeExternalSrcMsg;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.processor.ProcessorSupplier;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StreamController extends StreamsService {

  private final Topic<ByteKey<EpisodeMsgKey>, KissEpisodeExternalSrcMsg> kissExternalSourceTopic;
  private final ProcessorSupplier<ByteKey<EpisodeMsgKey>, KissEpisodeExternalSrcMsg> episodeProcessorSupplier;

  @Override
  protected Topology buildTopology() {
    StreamsBuilder builder = new StreamsBuilder();

    builder.stream(kissExternalSourceTopic.getName(), kissExternalSourceTopic.consumedWith())
        .filter((k, v) -> v.getSource().equals(ExternalSources.NOVA))
        .process(episodeProcessorSupplier);

    return builder.build();
  }
}
