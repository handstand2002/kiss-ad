package com.brokencircuits.kissad.kafka.table;


import com.brokencircuits.kissad.kafka.Topic;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;

@Slf4j
public class GlobalConsumerThread extends Thread {

  private final Consumer<byte[], byte[]> consumer;
  private final Collection<Topic<?, ?>> topics;
  private final Map<String, Collection<RecordProcessAction<?, ?>>> recordHandlers;
  private final Map<String, Topic<?, ?>> topicMap;
  private final BlockingQueue<ConsumerRequest<?>> requests = new LinkedBlockingQueue<>();
  private final Collection<java.util.function.Consumer<Consumer<byte[], byte[]>>> persistentRequests = new HashSet<>();

  private final Properties consumerProps;
  @Getter
  private ConsumerThreadState streamThreadState;
  private Duration consumerPoll;

  public GlobalConsumerThread(String name, ConsumerProvider consumerProvider,
                              Collection<Topic<?, ?>> topics,
                              Map<String, Collection<RecordProcessAction<?, ?>>> recordHandlers,
                              Properties consumerProps) {
    super(name);
    this.topics = topics;
    this.recordHandlers = new HashMap<>(recordHandlers);
    this.topicMap = topics.stream().collect(Collectors.toMap(Topic::getName, f -> f));
    this.consumerProps = consumerProps;
    this.consumer = consumerProvider.get(globalProps(consumerProps));
    streamThreadState = ConsumerThreadState.CREATED;
    parseConsumerProps();
  }

  public <V> Future<V> accessConsumer(Function<Consumer<byte[], byte[]>, V> runFunction) {
    CompletableFuture<V> future = new CompletableFuture<>();

    if (streamThreadState == ConsumerThreadState.CREATED) {
      future.complete(runFunction.apply(consumer));
    } else {
      requests.add(new ConsumerRequest<>(runFunction, future));
    }
    return future;
  }

  public java.util.function.Consumer<Consumer<byte[], byte[]>> registerPersistentRequest(
      java.util.function.Consumer<Consumer<byte[], byte[]>> request) {
    persistentRequests.add(request);
    return request;
  }

  public void unregisterPersistentRequest(
      java.util.function.Consumer<Consumer<byte[], byte[]>> request) {
    persistentRequests.remove(request);
  }

  private Properties globalProps(Properties consumerProps) {
    Properties newProps = new Properties(consumerProps);
    newProps.remove("group.id");
    newProps.put("auto.offset.reset", "earliest");
    newProps.put("enable.auto.commit", "false");
    newProps.put("key.deserializer", ByteArrayDeserializer.class);
    newProps.put("value.deserializer", ByteArrayDeserializer.class);
    return newProps;
  }

  private void parseConsumerProps() {
    this.consumerPoll = getProperty("poll.ms", Duration.ofMillis(100), Duration::parse);
  }

  private <T> T getProperty(String propName, T defaultValue, Function<String, T> parser) {
    String propValue = consumerProps.getProperty(propName);
    if (propValue == null) {
      return defaultValue;
    }
    return parser.apply(propValue);
  }

  public void triggerShutdown() {
    updateState(ConsumerThreadState.PENDING_SHUTDOWN);
  }

  @Override
  public void run() {
    updateState(ConsumerThreadState.STARTING);
    if (topics.isEmpty()) {
      throw new IllegalStateException("No topics for thread to consume");
    }
    assignConsumerToTopics();
    updateState(ConsumerThreadState.RUNNING);

    do {
      consumer.poll(consumerPoll).forEach(this::handleRecord);

      if (!requests.isEmpty()) {
        BlockingQueue<ConsumerRequest<?>> serviceRequests = new LinkedBlockingQueue<>();
        requests.drainTo(serviceRequests);

        for (ConsumerRequest<?> request : serviceRequests) {
          try {
            serviceRequest(request, consumer);
          } catch (Exception e) {
            request.getFuture().completeExceptionally(e);
          }
        }
      }

      if (!persistentRequests.isEmpty()) {
        persistentRequests.forEach(request -> request.accept(consumer));
      }

    } while (streamThreadState != ConsumerThreadState.PENDING_SHUTDOWN);

    updateState(ConsumerThreadState.SHUTDOWN);
  }

  private <T> void serviceRequest(ConsumerRequest<T> request, Consumer<byte[], byte[]> consumer) {
    request.getFuture().complete(request.getRequest().apply(consumer));
  }

  private void updateState(ConsumerThreadState newState) {
    log.debug("State update {}=>{}", streamThreadState, newState);
    streamThreadState = newState;
  }

  private <K, V> void handleRecord(ConsumerRecord<byte[], byte[]> record) {
    Topic<K, V> topic = getTopic(record.topic());
    if (recordHandlerFor(record).isEmpty()) {
      // don't deserialize if no handlers
      return;
    }

    K key = topic.getKeySerde().deserializer().deserialize(topic.getName(), record.key());
    V value = topic.getValueSerde().deserializer().deserialize(topic.getName(), record.value());

    ConsumerRecord<K, V> newRecord = new ConsumerRecord<>(record.topic(), record.partition(),
        record.offset(), record.timestamp(),
        record.timestampType(), record.checksum(), record.serializedKeySize(),
        record.serializedValueSize(), key, value, record.headers());

    recordHandlerFor(newRecord).forEach(action -> action.apply(newRecord));
  }

  private <K, V> Topic<K, V> getTopic(String topic) {
    return (Topic<K, V>) topicMap.get(topic);
  }

  @SuppressWarnings("unchecked,rawtypes")
  private <V, K> Collection<RecordProcessAction<K, V>> recordHandlerFor(
      ConsumerRecord<K, V> record) {
    return (Collection) recordHandlers
        .getOrDefault(record.topic(), Collections.emptyList());
  }

  private void assignConsumerToTopics() {
    List<TopicPartition> partitions = topics.stream()
        .map(Topic::getName)
        .map(consumer::partitionsFor)
        .flatMap(this::partitionInfo)
        .collect(Collectors.toList());

    consumer.assign(partitions);
    consumer.seekToBeginning(partitions);
  }

  private Stream<TopicPartition> partitionInfo(List<PartitionInfo> partitions) {
    return partitions.stream().map(i -> new TopicPartition(i.topic(), i.partition()));
  }
}
