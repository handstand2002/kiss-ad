package com.brokencircuits.kissad.hsshowfetch.streams;

import com.brokencircuits.kissad.kafka.KeyValueStore;
import com.brokencircuits.kissad.kafka.StreamsService;
import com.brokencircuits.kissad.kafka.Topic;
import com.brokencircuits.kissad.messages.ShowMessage;
import java.util.Collection;
import java.util.Properties;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Materialized;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class StreamController extends StreamsService {

  private final Properties streamProperties;
  private final Collection<KeyValueStore<?, ?>> stores;

  @Override
  protected void afterStreamsStart(KafkaStreams streams) {
    stores.forEach(store -> store.initialize(streams));
  }

  @Override
  protected KafkaStreams getStreams() {
    return new KafkaStreams(buildTopology(), streamProperties);
  }

  private Topology buildTopology() {

    // build all the global stores for KeyValueStore objects
    stores.forEach(store -> streamsBuilder
        .globalTable(store.getBuiltOnTopic().getName(), store.getBuiltOnTopic().consumed(),
            Materialized.as(store.getName())));

    return streamsBuilder.build();
  }
}
