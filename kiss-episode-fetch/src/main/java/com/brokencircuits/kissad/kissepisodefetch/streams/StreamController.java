package com.brokencircuits.kissad.kissepisodefetch.streams;

import com.brokencircuits.kissad.kafka.ByteKey;
import com.brokencircuits.kissad.kafka.KeyValueStoreWrapper;
import com.brokencircuits.kissad.kafka.StreamsService;
import com.brokencircuits.kissad.kafka.Topic;
import com.brokencircuits.kissad.messages.KissEpisodePageKey;
import com.brokencircuits.kissad.messages.KissEpisodePageMessage;
import com.brokencircuits.messages.KissCaptchaBatchKeywordKey;
import com.brokencircuits.messages.KissCaptchaMatchedKeywordMsg;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.processor.ProcessorSupplier;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StreamController extends StreamsService {

  private final KeyValueStoreWrapper<ByteKey<KissCaptchaBatchKeywordKey>, KissCaptchaMatchedKeywordMsg> matchedKeywordsStore;
  private final Topic<ByteKey<KissEpisodePageKey>, KissEpisodePageMessage> kissEpisodePageQueueTopic;
  private final ProcessorSupplier<ByteKey<KissEpisodePageKey>, KissEpisodePageMessage> episodeProcessorSupplier;

  @Override
  protected void afterStreamsStart(KafkaStreams streams) {
    matchedKeywordsStore.initialize(streams);
  }

  @Override
  protected Topology buildTopology() {
    StreamsBuilder builder = new StreamsBuilder();
    matchedKeywordsStore.addToBuilder(builder);

    builder.stream(kissEpisodePageQueueTopic.getName(), kissEpisodePageQueueTopic.consumedWith())
        .process(episodeProcessorSupplier);

    return builder.build();
  }

}
