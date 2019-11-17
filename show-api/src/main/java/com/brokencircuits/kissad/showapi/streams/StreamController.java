package com.brokencircuits.kissad.showapi.streams;

import com.brokencircuits.kissad.kafka.ClusterConnectionProps;
import com.brokencircuits.kissad.kafka.KeyValueStoreWrapper;
import com.brokencircuits.kissad.kafka.StreamsService;
import com.brokencircuits.kissad.messages.ShowMsgKey;
import com.brokencircuits.kissad.messages.ShowMsgValue;
import com.brokencircuits.kissad.showapi.Intercept;
import java.util.Collection;
import java.util.Properties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class StreamController extends StreamsService {

  private final ClusterConnectionProps clusterConnectionProps;
  private final KeyValueStoreWrapper<ShowMsgKey, ShowMsgValue> showStoreWrapper;
//  private final Collection<KeyValueStoreWrapper<?, ?>> stores;

  @Override
  protected void afterStreamsStart(KafkaStreams streams) {
//    stores.forEach(store -> store.initialize(streams));
    ReadOnlyKeyValueStore<Object, Object> store = streams
        .store(showStoreWrapper.getStoreName(), QueryableStoreTypes.keyValueStore());
    log.info("Initialized store");
  }

  @Override
  protected KafkaStreams getStreams() {
    Properties props = clusterConnectionProps.asProperties();
    props.put(ConsumerConfig.INTERCEPTOR_CLASSES_CONFIG, Intercept.class.getName());

    return new KafkaStreams(buildTopology(), props);
  }

  private Topology buildTopology() {
    builder = new StreamsBuilder();

    builder.globalTable(showStoreWrapper.getBuiltOnTopic().getName(),
        showStoreWrapper.getBuiltOnTopic().consumedWith(), Materialized
            .as(showStoreWrapper.getStoreName()));
//    stores.forEach(store -> store.addToBuilder(builder));

    return builder.build();
  }

}
