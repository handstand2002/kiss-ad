package com.brokencircuits.kissad.streamshowfetch.streams;

import com.brokencircuits.kissad.kafka.StreamsService;
import com.brokencircuits.kissad.kafka.Topic;
import com.brokencircuits.kissad.messages.KissShowMessage;
import com.brokencircuits.kissad.streamshowfetch.streamprocessing.ShowMessageProcessor;
import java.util.Properties;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.Topology;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class ShowStreams extends StreamsService {

  private final Properties streamProperties;
  private final Topic<Long, KissShowMessage> showTopic;
  private final ShowMessageProcessor showMessageProcessor;

  @Override
  protected KafkaStreams getStreams() {
    return new KafkaStreams(buildTopology(), streamProperties);
  }

  private Topology buildTopology() {
    streamsBuilder.stream(showTopic.getName(), showTopic.consumed())
        .process(() -> showMessageProcessor);

    return streamsBuilder.build();
  }
}
