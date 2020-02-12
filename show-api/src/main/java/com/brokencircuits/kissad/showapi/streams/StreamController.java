package com.brokencircuits.kissad.showapi.streams;

import com.brokencircuits.kissad.kafka.ClusterConnectionProps;
import com.brokencircuits.kissad.kafka.StreamsService;
import com.brokencircuits.kissad.kafka.Topic;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Transformer;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.state.Stores;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class StreamController extends StreamsService {

  private final ClusterConnectionProps clusterConnectionProps;
  private final Topic<String, String> inTopic;
  private final Topic<String, String> repartition1Topic;
  private final Topic<String, String> repartition2Topic;
  private final Topic<String, String> repartition3Topic;
  private final Topic<String, String> outTopic;
  private final static String storeName = "STORE";


  @Override
  protected KafkaStreams getStreams() {
    return new KafkaStreams(buildTopology(), clusterConnectionProps.asProperties());
  }

  private Topology buildTopology() {
    builder = new StreamsBuilder();

    builder.addStateStore(Stores
        .keyValueStoreBuilder(Stores.persistentKeyValueStore(storeName), inTopic.getKeySerde(),
            inTopic.getValueSerde()));

    builder.stream(inTopic.getName(), inTopic.consumedWith())
        .peek((k, v) -> log.info("Checkpoint 1: {} | {}", k, v))
        .transform(PassthroughTransformer::new, storeName)
        .through(repartition1Topic.getName(), repartition1Topic.producedWith())
        .peek((k, v) -> log.info("Checkpoint 2: {} | {}", k, v))
        .transform(PassthroughTransformer::new, storeName)
        .through(repartition2Topic.getName(), repartition2Topic.producedWith())
        .peek((k, v) -> log.info("Checkpoint 3: {} | {}", k, v))
        .transform(PassthroughTransformer::new, storeName)
        .through(repartition3Topic.getName(), repartition3Topic.producedWith())
        .peek((k, v) -> log.info("Checkpoint 4: {} | {}", k, v))
        .transform(PassthroughTransformer::new, storeName)
        .peek((k, v) -> log.info("Checkpoint 5: {} | {}", k, v))
        .to(outTopic.getName(), outTopic.producedWith());

    Topology topology = builder.build();
    log.info("Topology: {}", topology.describe());
    return topology;
  }

  private static class PassthroughTransformer implements
      Transformer<String, String, KeyValue<String, String>> {

    ProcessorContext context;

    @Override
    public void init(ProcessorContext context) {
      this.context = context;
    }

    @Override
    public KeyValue<String, String> transform(String key, String value) {
      log.info("{} Processing {} | {}", context.taskId(), key, value);
      return KeyValue.pair(key, value);
    }

    @Override
    public void close() {

    }
  }

}
