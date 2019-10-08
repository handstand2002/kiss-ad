package com.brokencircuits.kissad.hsshowfetch.streams;

import com.brokencircuits.kissad.kafka.KafkaProperties;
import com.brokencircuits.kissad.kafka.StreamsService;
import com.brokencircuits.kissad.kafka.Topic;
import com.brokencircuits.kissad.messages.ShowMsgKey;
import com.brokencircuits.kissad.messages.ShowMsgValue;
import com.brokencircuits.kissad.messages.SourceName;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.processor.ProcessorSupplier;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StreamController extends StreamsService {

  private final KafkaProperties streamProperties;
  private final Topic<ShowMsgKey, ShowMsgValue> showQueueTopic;
  private final ProcessorSupplier<ShowMsgKey, ShowMsgValue> showProcessorSupplier;

  @Override
  protected KafkaStreams getStreams() {
    return new KafkaStreams(buildTopology(), streamProperties);
  }

  private Topology buildTopology() {
    builder = new StreamsBuilder();

    KStream<ShowMsgKey, ShowMsgValue> showStream = builder
        .stream(showQueueTopic.getName(), showQueueTopic.consumedWith())
        .peek(StreamsService::logConsume);

    KStream<ShowMsgKey, ShowMsgValue>[] branches = showStream.branch(
        (key, value) -> value.getSources().containsKey(SourceName.HORRIBLESUBS.name()),
        (key, value) -> true
    );

    branches[1].foreach((key, value) -> log.info("Ignoring show as it does not have a valid "
        + "source for this fetcher: {} | {}", key, value));

    branches[0].process(showProcessorSupplier);

    return builder.build();
  }

  private void processShow(ShowMsgKey key, ShowMsgValue value) {

  }


}
