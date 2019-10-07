package com.brokencircuits.kissad.kafka;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.processor.Processor;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.logging.log4j.util.BiConsumer;
import org.apache.logging.log4j.util.TriConsumer;

@Slf4j
@Builder
public class TrivialProcessor<K, V> implements Processor<K, V> {

  private TriConsumer<K, V, V> onRecordActionWithPrevious;
  private BiConsumer<K,V> onRecordAction;
  private String storeName;
  @Builder.Default
  private KeyValueStore<K, V> internalStore;

  @Override
  public void init(ProcessorContext context) {
    if (storeName != null) {
      internalStore = (KeyValueStore) context.getStateStore(storeName);
    }
  }

  @Override
  public void process(K key, V newValue) {

    log.debug("Processing {} | {}", key, newValue);
    V oldValue = null;
    if (storeName != null) {
      log.debug("Updating store with new value: {} | {}", key, newValue);
      oldValue = internalStore.get(key);
      internalStore.put(key, newValue);
    }

    if (onRecordActionWithPrevious != null) {
      try {
        onRecordActionWithPrevious.accept(key, oldValue, newValue);
      } catch (RuntimeException e) {
        log.error("Exception in onRecordActionWithPrevious: ", e);
      }
    }

    if (onRecordAction != null) {
      try {
        onRecordAction.accept(key, newValue);
      } catch (RuntimeException e) {
        log.error("Exception in onRecordAction: ", e);
      }
    }
    log.debug("Finished processing {} | {}", key, newValue);
  }

  @Override
  public void close() {

  }
}
