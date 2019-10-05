package com.brokencircuits.kissad.showapi.streams;

import com.brokencircuits.kissad.kafka.KeyValueStore;
import com.brokencircuits.kissad.kafka.StreamsService;
import java.util.Collection;
import java.util.Properties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Materialized;
import org.springframework.stereotype.Component;

@Slf4j
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
    streamsBuilder = new StreamsBuilder();

    // build all the global stores for KeyValueStore objects
    stores.forEach(store -> streamsBuilder
        .globalTable(store.getBuiltOnTopic().getName(), store.getBuiltOnTopic().consumedWith(),
            Materialized.as(store.getName())));

    return streamsBuilder.build();
  }

}
