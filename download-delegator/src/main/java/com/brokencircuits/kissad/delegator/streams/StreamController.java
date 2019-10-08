package com.brokencircuits.kissad.delegator.streams;

import com.brokencircuits.downloader.messages.DownloadStatusKey;
import com.brokencircuits.downloader.messages.DownloadStatusValue;
import com.brokencircuits.kissad.download.DownloadApi;
import com.brokencircuits.kissad.kafka.KafkaProperties;
import com.brokencircuits.kissad.kafka.StateStoreDetails;
import com.brokencircuits.kissad.kafka.StreamsService;
import com.brokencircuits.kissad.kafka.Topic;
import com.brokencircuits.kissad.kafka.TrivialProcessor;
import com.brokencircuits.kissad.kafka.Util;
import com.brokencircuits.kissad.messages.EpisodeMsgKey;
import com.brokencircuits.kissad.messages.EpisodeMsgValue;
import com.brokencircuits.kissad.messages.ShowMsgKey;
import com.brokencircuits.kissad.messages.ShowMsgValue;
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
  private final KafkaProperties streamProperties;
  private final StateStoreDetails<EpisodeMsgKey, EpisodeMsgValue> notDownloadedStoreDetails;
  private final StateStoreDetails<ShowMsgKey, ShowMsgValue> showStoreDetails;
  private final Topic<EpisodeMsgKey, EpisodeMsgValue> episodeQueueTopic;
  private final Topic<DownloadStatusKey, DownloadStatusValue> downloadStatusTopic;
  private final Topic<ShowMsgKey, ShowMsgValue> showStoreTopic;
  private final ProcessorSupplier<EpisodeMsgKey, EpisodeMsgValue> episodeProcessorSupplier;

  @Override
  protected KafkaStreams getStreams() {
    return new KafkaStreams(buildTopology(), streamProperties);
  }

  Topology buildTopology() {
    builder = new StreamsBuilder();

    builder.addStateStore(Util.keyValueStoreBuilder(notDownloadedStoreDetails));

    builder.stream(episodeQueueTopic.getName(), episodeQueueTopic.consumedWith())
        .peek(StreamsService::logConsume)
        .process(episodeProcessorSupplier, notDownloadedStoreDetails.getStoreName());

    builder.stream(downloadStatusTopic.getName(), downloadStatusTopic.consumedWith())
        .foreach(downloadApi::onDownloadStatusMessage);

    KeyValueStoreBuilder<ShowMsgKey, ShowMsgValue> showStoreBuilder = Util
        .keyValueStoreBuilder(showStoreDetails);

    builder
        .addGlobalStore(showStoreBuilder, showStoreTopic.getName(), showStoreTopic.consumedWith(),
            () -> TrivialProcessor.<ShowMsgKey, ShowMsgValue>builder()
                .storeName(showStoreDetails.getStoreName()).build());

    return builder.build();
  }

}
