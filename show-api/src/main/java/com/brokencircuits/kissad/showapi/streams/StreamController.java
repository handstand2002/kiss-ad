package com.brokencircuits.kissad.showapi.streams;

import com.brokencircuits.kissad.kafka.ClusterConnectionProps;
import com.brokencircuits.kissad.kafka.KeyValueStoreWrapper;
import com.brokencircuits.kissad.kafka.StreamsService;
import java.util.Collection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class StreamController extends StreamsService {

  private final ClusterConnectionProps clusterConnectionProps;
  private final Collection<KeyValueStoreWrapper<?, ?>> stores;

  @Override
  protected void afterStreamsStart(KafkaStreams streams) {
    stores.forEach(store -> store.initialize(streams));
  }

  @Override
  protected KafkaStreams getStreams() {
    return new KafkaStreams(buildTopology(), clusterConnectionProps.asProperties());
  }

  private Topology buildTopology() {
    builder = new StreamsBuilder();

    stores.forEach(store -> store.addToBuilder(builder));

    return builder.build();
  }

}
