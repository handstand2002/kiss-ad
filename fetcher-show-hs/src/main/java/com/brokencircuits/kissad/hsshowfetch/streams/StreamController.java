package com.brokencircuits.kissad.hsshowfetch.streams;

import com.brokencircuits.kissad.kafka.ByteKey;
import com.brokencircuits.kissad.kafka.ClusterConnectionProps;
import com.brokencircuits.kissad.kafka.StreamsService;
import com.brokencircuits.kissad.kafka.Topic;
import com.brokencircuits.kissad.messages.ShowMsg;
import com.brokencircuits.kissad.messages.ShowMsgKey;
import com.brokencircuits.kissad.messages.SourceName;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.processor.ProcessorSupplier;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StreamController extends StreamsService {

  private final ClusterConnectionProps clusterConnectionProps;
  private final Topic<ByteKey<ShowMsgKey>, ShowMsg> showQueueTopic;
  private final ProcessorSupplier<ByteKey<ShowMsgKey>, ShowMsg> showProcessorSupplier;

  protected Topology buildTopology() {
    StreamsBuilder builder = new StreamsBuilder();

    KStream<ByteKey<ShowMsgKey>, ShowMsg> showStream = builder
        .stream(showQueueTopic.getName(), showQueueTopic.consumedWith())
        .peek(StreamsService::logConsume);

    KStream<ByteKey<ShowMsgKey>, ShowMsg>[] branches = showStream.branch(
        (key, msg) -> msg.getValue().getSources().containsKey(SourceName.HORRIBLESUBS.name()),
        (key, msg) -> true
    );

    branches[1].foreach((key, value) -> log.info("Ignoring show as it does not have a valid "
        + "source for this fetcher: {} | {}", key, value));

    branches[0].process(showProcessorSupplier);

    return builder.build();
  }

}
