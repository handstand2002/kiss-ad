package com.brokencircuits.kissad.kafka.table;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import lombok.Value;
import org.apache.kafka.clients.consumer.Consumer;

@Value
public class ConsumerRequest<T> {

  Function<Consumer<byte[], byte[]>, T> request;
  CompletableFuture<T> future;
}
