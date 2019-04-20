package com.brokencircuits.kissad.restshow.streams;

import com.brokencircuits.kissad.kafka.KeyValueStore;
import com.brokencircuits.kissad.kafka.StreamsService;
import com.brokencircuits.kissad.kafka.Topic;
import com.brokencircuits.kissad.messages.ShowMessage;
import java.util.Properties;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Materialized;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class ShowStreams extends StreamsService {

  private final Properties streamProperties;
  private final Topic<Long, ShowMessage> showTopic;
  private final KeyValueStore<Long, ShowMessage> showMessageStore;

  @Override
  protected void afterStreamsStart(KafkaStreams streams) {
    showMessageStore.initialize(streams);
  }

  @Override
  protected KafkaStreams getStreams() {
    return new KafkaStreams(buildTopology(), streamProperties);
  }

  private Topology buildTopology() {
    streamsBuilder.globalTable(showTopic.getName(), showTopic.consumed(),
        Materialized.as(showMessageStore.getName()));

    return streamsBuilder.build();
  }
}
