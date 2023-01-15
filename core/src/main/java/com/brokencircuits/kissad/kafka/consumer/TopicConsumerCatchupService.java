package com.brokencircuits.kissad.kafka.consumer;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.apache.kafka.common.TopicPartition;

public class TopicConsumerCatchupService {

  private final Semaphore catchupSemaphore;
  private final Consumer<org.apache.kafka.clients.consumer.Consumer<byte[], byte[]>> persistentRequest;
  private final GlobalConsumerThread consumerThread;

  public TopicConsumerCatchupService(GlobalConsumerThread consumerThread, String topicName)
      throws ExecutionException, InterruptedException {
    this.consumerThread = consumerThread;
    Map<TopicPartition, Long> endOffsets = consumerThread.accessConsumer(api -> {
      List<TopicPartition> partitions = api.partitionsFor(topicName)
          .stream()
          .map(p -> new TopicPartition(p.topic(), p.partition()))
          .collect(Collectors.toList());

      return api.endOffsets(partitions);
    }).get();

    catchupSemaphore = new Semaphore(0);
    persistentRequest = consumerThread
        .registerPersistentRequest(api -> {
          Set<TopicPartition> caughtUpPartitions = new HashSet<>();
          endOffsets.forEach((topicPartition, endOffset) -> {
            if (api.position(topicPartition) >= endOffset) {
              caughtUpPartitions.add(topicPartition);
            }
          });
          caughtUpPartitions.forEach(endOffsets::remove);

          if (endOffsets.isEmpty()) {
            catchupSemaphore.release();
          }
        });
  }

  public void waitForCatchup() throws InterruptedException {
    catchupSemaphore.acquire();
    consumerThread.unregisterPersistentRequest(persistentRequest);
  }
}
