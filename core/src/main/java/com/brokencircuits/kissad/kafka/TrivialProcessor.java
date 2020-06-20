package com.brokencircuits.kissad.kafka;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import lombok.Builder;
import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.Schema.Field;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.processor.Processor;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.logging.log4j.util.BiConsumer;
import org.apache.logging.log4j.util.TriConsumer;

@Slf4j
@Builder
public class TrivialProcessor<K, V extends SpecificRecordBase> implements Processor<K, V> {

  private TriConsumer<K, V, V> onRecordActionWithPrevious;
  private BiConsumer<K, V> onRecordAction;
  private Predicate<KeyValue<K, V>> expiryPredicate;    // TODO: figure out how to get rid of old entries
  private String storeName;

  private final AtomicReference<KeyValueStore<K, V>> internalStore = new AtomicReference<>(null);

  @Override
  public void init(ProcessorContext context) {
    if (storeName != null) {
      internalStore.set((KeyValueStore) context.getStateStore(storeName));
    }
  }

  private void punctuate(long timestamp) {
    if (internalStore.get() != null) {
      @Cleanup
      KeyValueIterator<K, V> iter = internalStore.get().all();
      iter.forEachRemaining(entry -> {
        if (expiryPredicate.test(entry)) {
          internalStore.get().delete(entry.key);
        }
      });
    }
  }

  @Override
  public void process(K key, V newValue) {

    log.debug("Processing {} | {}", key, newValue);
    V oldValue = null;
    if (storeName != null) {
      log.info("Updating store {}: {} | {}", storeName, key, newValue);
      oldValue = internalStore.get().get(key);

      Object nestedValue = newValue;
      if (newValue != null) {
        Field valueField = newValue.getSchema().getField("value");
        if (valueField != null) {
          nestedValue = newValue.get("value");
        }
      }
      if (nestedValue == null) {
        internalStore.get().delete(key);
      } else {
        internalStore.get().put(key, newValue);
      }
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
