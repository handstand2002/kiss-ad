package com.brokencircuits.kissad.kafka;

import com.brokencircuits.kissad.util.Service;
import com.brokencircuits.kissad.util.Uuid;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.kafka.support.TopicPartitionInitialOffset.SeekPosition;

@Slf4j
public class TopicMap<K, V> implements Service, Map<K, V> {

  private final AtomicLong messageUpdateCounter = new AtomicLong(0);
  private static final String MSG_HEADER_ID_KEY = "MSG_ID";
  private final Topic<K, V> topic;
  private final ClusterConnectionProps kafkaProperties;
  private final Producer<K, V> producer;
  private final AnonymousConsumer<K, V> consumer;
  private final Map<K, KafkaRecordValue<V>> innerMap = new ConcurrentHashMap<>();
  private final Map<K, KafkaRecordValue<V>> dirtyKeys = new ConcurrentHashMap<>();
  private final Map<K, KafkaRecordValue<V>> updatedRecords = new ConcurrentHashMap<>();

  public TopicMap(Topic<K, V> topic, ClusterConnectionProps kafkaProperties) {
    this.topic = topic;
    this.kafkaProperties = kafkaProperties;

    producer = new KafkaProducer<>(kafkaProperties.asProperties(),
        topic.getKeySerde().serializer(), topic.getValueSerde().serializer());
    consumer = new AnonymousConsumer<>(topic, kafkaProperties, SeekPosition.BEGINNING);
    consumer.setMessageListener(receivedRecord -> {
      KafkaRecordValue<V> valueWithMetadata = new KafkaRecordValue<>(receivedRecord.value(),
          receivedRecord.timestamp(), receivedRecord.headers(), receivedRecord.topic(),
          receivedRecord.partition(),
          receivedRecord.offset());
      log.debug("Received update to TopicMap: {} | {}", receivedRecord.key(),
          receivedRecord.value());

      if (mapContainsMsg(dirtyKeys, receivedRecord)) {
        log.debug("Removed pending delete for {}", receivedRecord.key());
        innerMap.remove(receivedRecord.key());
        dirtyKeys.remove(receivedRecord.key());

      } else if (mapContainsMsg(updatedRecords, receivedRecord)) {
        log.debug("Removed pending update for {} | {}", receivedRecord.key(),
            receivedRecord.value());
        innerMap.put(receivedRecord.key(), valueWithMetadata);
        updatedRecords.remove(receivedRecord.key());

      } else if (receivedRecord.value() == null) {
        log.debug("Received anonymous delete for {}", receivedRecord.key());
        innerMap.remove(receivedRecord.key());

      } else {
        log.debug("Received anonymous update for {} | {}", receivedRecord.key(),
            receivedRecord.value());
        innerMap.put(receivedRecord.key(), valueWithMetadata);
      }
    });
  }

  private boolean mapContainsMsg(Map<K, KafkaRecordValue<V>> map,
      ConsumerRecord<K, V> receivedRecord) {
    KafkaRecordValue<V> storedValueWithMeta = map.get(receivedRecord.key());
    if (storedValueWithMeta != null) {
      byte[] receivedRecordMsgId = StreamSupport
          .stream(receivedRecord.headers().headers(MSG_HEADER_ID_KEY).spliterator(), false)
          .findFirst().map(Header::value)
          .orElse(null);

      return StreamSupport
          .stream(storedValueWithMeta.getHeaders().headers(MSG_HEADER_ID_KEY).spliterator(), false)
          .anyMatch(header -> Arrays.equals(header.value(), receivedRecordMsgId));
    }
    return false;
  }

  @Override
  public void start() throws Exception {
    consumer.start();
  }

  @Override
  public void stop() {
    consumer.stop();
  }

  /**
   * Returns the number of entries in the main map, only including entries that were received from
   * kafka
   */
  @Override
  public int size() {
    return keySet().size();
  }

  @Override
  public boolean isEmpty() {
    return keySet().isEmpty();
  }

  @Override
  public boolean containsKey(Object key) {
    return get(key) != null;
  }

  @Override
  public boolean containsValue(Object value) {
    return values().contains(value);
  }

  @Override
  public V get(Object key) {
    Optional<KafkaRecordValue<V>> latestUpdate = Optional.ofNullable(updatedRecords.get(key));
    Optional<KafkaRecordValue<V>> latestDelete = Optional.ofNullable(dirtyKeys.get(key));
    Long updateOffset = latestUpdate.map(KafkaRecordValue::getOffset).orElse(-1L);
    Long deleteOffset = latestDelete.map(KafkaRecordValue::getOffset).orElse(-1L);
    log.debug("GET: updateOffset: {}; deleteOffset: {}", updateOffset, deleteOffset);
    if (updateOffset > -1 || deleteOffset > -1) {
      return deleteOffset > updateOffset ? null : latestUpdate.get().getValue();
    }
    return Optional.ofNullable(innerMap.get(key)).map(KafkaRecordValue::getValue).orElse(null);
  }

  @Override
  public V put(K key, V value) {
    V previousValue = get(key);
    publish(key, value);
    return previousValue;
  }

  private void publish(K key, V value) {
    ProducerRecord<K, V> record = new ProducerRecord<>(topic.getName(), key, value);
    record.headers().add(MSG_HEADER_ID_KEY, Uuid.randomUUID().toString().getBytes());
    KafkaRecordValue<V> internalMapValue = new KafkaRecordValue<>(value, -1, record.headers(),
        record.topic(), record.partition(), messageUpdateCounter.incrementAndGet());
    if (value == null) {
      dirtyKeys.put(record.key(), internalMapValue);
    } else {
      updatedRecords.put(record.key(), internalMapValue);
    }
    log.info("Publishing: {}", record);
    producer.send(record);
  }

  @Override
  public V remove(Object key) {
    return put((K) key, null);
  }

  @Override
  public void putAll(Map<? extends K, ? extends V> m) {
    m.forEach(this::put);
  }

  @Override
  public void clear() {
    innerMap.keySet().forEach(this::remove);
  }

  @Override
  public Set<K> keySet() {
    HashSet<K> ks = new HashSet<>(innerMap.keySet());
    ks.addAll(updatedRecords.keySet());
    ks.removeIf(k -> get(k) == null);
    return ks;
  }

  @Override
  public Collection<V> values() {
    return keySet().stream().map(this::get).collect(Collectors.toList());
  }

  @Override
  public Set<Entry<K, V>> entrySet() {
    return keySet().stream().collect(Collectors.toMap(k -> k, this::get)).entrySet();
  }
}
