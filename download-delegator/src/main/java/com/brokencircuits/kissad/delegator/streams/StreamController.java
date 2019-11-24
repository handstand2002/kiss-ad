package com.brokencircuits.kissad.delegator.streams;

import com.brokencircuits.downloader.messages.DownloadStatusKey;
import com.brokencircuits.downloader.messages.DownloadStatusMsg;
import com.brokencircuits.kissad.download.DownloadApi;
import com.brokencircuits.kissad.kafka.ByteKey;
import com.brokencircuits.kissad.kafka.ClusterConnectionProps;
import com.brokencircuits.kissad.kafka.StateStoreDetails;
import com.brokencircuits.kissad.kafka.StreamsService;
import com.brokencircuits.kissad.kafka.Topic;
import com.brokencircuits.kissad.kafka.TrivialProcessor;
import com.brokencircuits.kissad.kafka.Util;
import com.brokencircuits.kissad.messages.EpisodeMsg;
import com.brokencircuits.kissad.messages.EpisodeMsgKey;
import com.brokencircuits.kissad.messages.ShowMsg;
import com.brokencircuits.kissad.messages.ShowMsgKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.processor.ProcessorSupplier;
import org.apache.kafka.streams.state.internals.KeyValueStoreBuilder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StreamController extends StreamsService {

  private final DownloadApi downloadApi;
  private final ClusterConnectionProps streamProperties;
  private final StateStoreDetails<ByteKey<EpisodeMsgKey>, EpisodeMsg> notDownloadedStoreDetails;
  private final StateStoreDetails<ByteKey<ShowMsgKey>, ShowMsg> showStoreDetails;
  private final StateStoreDetails<ByteKey<EpisodeMsgKey>, EpisodeMsg> episodeStoreDetails;
  private final Topic<ByteKey<EpisodeMsgKey>, EpisodeMsg> episodeQueueTopic;
  private final Topic<ByteKey<EpisodeMsgKey>, EpisodeMsg> episodeStoreTopic;
  private final Topic<ByteKey<DownloadStatusKey>, DownloadStatusMsg> downloadStatusTopic;
  private final Topic<ByteKey<ShowMsgKey>, ShowMsg> showStoreTopic;
  private final ProcessorSupplier<ByteKey<EpisodeMsgKey>, EpisodeMsg> episodeProcessorSupplier;

  @Override
  protected KafkaStreams getStreams() {
    return new KafkaStreams(buildTopology(), streamProperties.asProperties());
  }

  private Topology buildTopology() {
    builder = new StreamsBuilder();

    builder.addStateStore(Util.keyValueStoreBuilder(notDownloadedStoreDetails));

    builder.stream(episodeQueueTopic.getName(), episodeQueueTopic.consumedWith())
        .peek(StreamsService::logConsume)
        .process(episodeProcessorSupplier, notDownloadedStoreDetails.getStoreName());

    builder.stream(downloadStatusTopic.getName(), downloadStatusTopic.consumedWith())
        .foreach((k,v) -> downloadApi.onDownloadStatusMessage(v));

    KeyValueStoreBuilder<ByteKey<ShowMsgKey>, ShowMsg> showStoreBuilder = Util
        .keyValueStoreBuilder(showStoreDetails);
    builder.addGlobalStore(showStoreBuilder, showStoreTopic.getName(),
        showStoreTopic.consumedWith(), () -> TrivialProcessor.<ByteKey<ShowMsgKey>, ShowMsg>builder()
            .storeName(showStoreDetails.getStoreName()).build());

    KeyValueStoreBuilder<ByteKey<EpisodeMsgKey>, EpisodeMsg> episodeStoreBuilder = Util
        .keyValueStoreBuilder(episodeStoreDetails);
    builder.addGlobalStore(episodeStoreBuilder, episodeStoreTopic.getName(),
        episodeStoreTopic.consumedWith(),
        () -> TrivialProcessor.<ByteKey<EpisodeMsgKey>, EpisodeMsg>builder()
            .storeName(episodeStoreDetails.getStoreName()).build());

    return builder.build();
  }

}
