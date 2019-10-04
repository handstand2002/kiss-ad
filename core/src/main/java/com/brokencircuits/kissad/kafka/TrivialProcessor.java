package com.brokencircuits.kissad.kafka;

import java.util.function.BiConsumer;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.streams.processor.Processor;
import org.apache.kafka.streams.processor.ProcessorContext;

@RequiredArgsConstructor
public class TrivialProcessor<K,V> implements Processor<K, V> {

  final private BiConsumer<K,V> onRecord;

  @Override
  public void init(ProcessorContext processorContext) {

  }

  @Override
  public void process(K k, V v) {
    onRecord.accept(k, v);
  }

  @Override
  public void close() {

  }
}
