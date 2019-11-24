package com.brokencircuits.kissad.kafka;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.apache.logging.log4j.util.BiConsumer;
import org.apache.logging.log4j.util.TriConsumer;

@Slf4j
public class KeyValueStoreWrapper<K, V extends SpecificRecordBase> extends StateStoreDetails<K, V> implements
    ReadOnlyKeyValueStore<K, V> {

  @Getter
  final private Topic<K, V> builtOnTopic;

  private ReadOnlyKeyValueStore<K, V> store;

  public KeyValueStoreWrapper(String storeName, Topic<K, V> builtOnTopic) {
    super(storeName, builtOnTopic.getKeySerde(), builtOnTopic.getValueSerde());
    this.builtOnTopic = builtOnTopic;
  }

  public void initialize(KafkaStreams streams) {
    store = streams.store(getStoreName(), QueryableStoreTypes.keyValueStore());
    log.info("Initialized store '{}'", getStoreName());
  }

  private void checkStoreInitialized() {
    if (store == null) {
      throw new IllegalStateException("State store " + getStoreName() + " is not initialized yet");
    }
  }

  public StreamsBuilder addToBuilder(StreamsBuilder builder) {
    return addToBuilder(builder, (BiConsumer<K, V>) null);
  }

  public StreamsBuilder addToBuilder(StreamsBuilder builder,
      TriConsumer<K, V, V> onUpdateCallback) {
    return builder.addGlobalStore(Util.keyValueStoreBuilder(getStoreName(), builtOnTopic),
        builtOnTopic.getName(), builtOnTopic.consumedWith(),
        () -> TrivialProcessor.<K, V>builder().storeName(getStoreName())
            .onRecordActionWithPrevious(onUpdateCallback).build());
  }

  public StreamsBuilder addToBuilder(StreamsBuilder builder,
      BiConsumer<K, V> onUpdateCallback) {
    return builder.addGlobalStore(Util.keyValueStoreBuilder(getStoreName(), builtOnTopic),
        builtOnTopic.getName(), builtOnTopic.consumedWith(),
        () -> TrivialProcessor.<K, V>builder().storeName(getStoreName())
            .onRecordAction(onUpdateCallback).build());
  }

  @Override
  public V get(K k) {
    checkStoreInitialized();
    return store.get(k);
  }

  @Override
  public KeyValueIterator<K, V> range(K k, K k1) {
    checkStoreInitialized();
    return store.range(k, k1);
  }

  @Override
  public KeyValueIterator<K, V> all() {
    checkStoreInitialized();
    return store.all();
  }

  @Override
  public long approximateNumEntries() {
    checkStoreInitialized();
    return store.approximateNumEntries();
  }
}
