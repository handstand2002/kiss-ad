package com.brokencircuits.kissad.kafka.table;

import com.brokencircuits.kissad.kafka.Topic;
import com.brokencircuits.kissad.kafka.config.KafkaConfig;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.brokencircuits.kissad.kafka.consumer.ConsumeTopicDetails;
import com.brokencircuits.kissad.kafka.consumer.ConsumerProvider;
import com.brokencircuits.kissad.kafka.consumer.GlobalConsumerThread;
import com.brokencircuits.kissad.kafka.consumer.RecordProcessAction;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;

@Accessors(chain = true)
@RequiredArgsConstructor
@Builder
public class TableBuilder {

  private final ConsumerProvider consumerProvider;
  private final ProducerProvider producerProvider;
  private final StorageProvider storageProvider;
  private final KafkaConfig config;

  // TODO: use Bytes as key for live table too
//  public <K,V> KafkaLiveTable<K, V> liveTable(Topic<K, V> underlyingTopic, TableStorage<byte[], byte[]> storage) {
//
//    Map<String, ConsumeTopicDetails<?, ?>> recordHandlers = new HashMap<>();
//    recordHandlers.put(underlyingTopic.getName(), ConsumeTopicDetails.<K,V>builder()
//        .serializedAction(record -> storage.put(record.key(), record.value()))
//        .topic(underlyingTopic)
//        .build());
//
//    GlobalConsumerThread consumerThread = new GlobalConsumerThread(
//        String.format("%s-%s", KafkaLiveTable.class.getSimpleName(), underlyingTopic.getName()),
//        consumerProvider, recordHandlers, config.consumerProps());
//
//    return new KafkaLiveTable<K,V>(consumerThread, underlyingTopic, storage);
//  }

  public <K,V> KafkaBackedTable<K, V> backedTable(Topic<K, V> underlyingTopic) {
    return backedTable(underlyingTopic, null);
  }

    public <K,V> KafkaBackedTable<K, V> backedTable(Topic<K, V> underlyingTopic, TableStorage<Bytes, byte[]> storage) {

    if (storage == null) {
      storage = storageProvider.get(underlyingTopic.getName(), Serdes.Bytes(), Serdes.ByteArray());
    }

    // assignment to new variable necessary for usage in lambda below
    TableStorage<Bytes, byte[]> useStorage = storage;

    Map<String, ConsumeTopicDetails<?, ?>> recordHandlers = new HashMap<>();
    recordHandlers.put(underlyingTopic.getName(), ConsumeTopicDetails.<K,V>builder()
            .serializedAction(record -> useStorage.put(Bytes.wrap(record.key()), record.value()))
            .topic(underlyingTopic)
        .build());

    GlobalConsumerThread consumerThread = new GlobalConsumerThread(
        String.format("%s-%s", KafkaLiveTable.class.getSimpleName(), underlyingTopic.getName()),
        consumerProvider, recordHandlers, config.consumerProps());
    return new KafkaBackedTable<>(consumerThread, producerProvider.get(config.producerProps()), underlyingTopic, storage);
  }
}
