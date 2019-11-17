package com.brokencircuits.kissad.showapi;

import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerializer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecord;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.kafka.clients.consumer.ConsumerInterceptor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.ProducerInterceptor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;

@Slf4j
@RequiredArgsConstructor
public class Intercept<K extends SpecificRecord, V extends SpecificRecord> implements
    ConsumerInterceptor<byte[], byte[]>,
    ProducerInterceptor<K, V> {

  private static final String AVRO_KEY_HEADER = "AVRO_KEY_BYTES";
  private static final String AVRO_VALUE_HEADER = "AVRO_VALUE_BYTES";
  private final Serde<K> keySerde = new SpecificAvroSerde<>();
  private final Serde<V> valueSerde = new SpecificAvroSerde<>();
  private static final int headerBytes = 5;

  @Override
  public ConsumerRecords<byte[], byte[]> onConsume(
      ConsumerRecords<byte[], byte[]> consumerRecords) {
//
//    Map<TopicPartition, List<ConsumerRecord<byte[], byte[]>>> recordMap = new HashMap<>();
//
//    consumerRecords.forEach(record -> {
//      log.info("Consumed {}", record);
//      log.info("   Key: {}", record.key());
//      log.info("   Value: {}", record.value());
//      log.info("   Headers: {}", record.headers());
//      TopicPartition topicPartition = new TopicPartition(record.topic(), record.partition());
//      recordMap.putIfAbsent(topicPartition, new ArrayList<>());
//
//      byte[] updatedKey = ArrayUtils
//          .addAll(record.headers().lastHeader(AVRO_KEY_HEADER).value(), record.key());
//      byte[] updatedValue = ArrayUtils
//          .addAll(record.headers().lastHeader(AVRO_VALUE_HEADER).value(), record.value());
//
//      ConsumerRecord<byte[], byte[]> newRecord = new ConsumerRecord<>(record.topic(),
//          record.partition(),
//          record.offset(), record.timestamp(), record.timestampType(), -1, updatedKey.length,
//          updatedValue.length, updatedKey, updatedValue);
//
//      recordMap.get(topicPartition).add(newRecord);
//      log.info("Replacement record for consumption: {}", newRecord);
//      log.info("   Key: {}", newRecord.key());
//      log.info("   Value: {}", newRecord.value());
//      log.info("   Headers: {}", newRecord.headers());
//    });
//
//    return new ConsumerRecords<>(recordMap);
    return consumerRecords;
  }

  @Override
  public void onCommit(Map<TopicPartition, OffsetAndMetadata> map) {

  }

  /*
   * TODO: can't put the serialization in the interceptor because kafka streams uses interceptor
   *  at different points than expected. interceptor will have to serialize, grab the header bits
   *  and put on header, then return original record. original record will be serialized - need to
   *  write a serializer that will remove the header bits from the serialized bytes interceptor will
   *  take the bytes from header and re-append them to consumed object so deserialization can happen
   *  as normal
   * */

  @Override
  public ProducerRecord<K, V> onSend(ProducerRecord<K, V> record) {
//    log.info("Producing record: {}", record);
//    log.info("   Key: {}", record.key());
//    log.info("   Value: {}", record.value());
//    log.info("   Headers: {}", record.headers());
//
//    byte[] serializedKey = keySerde.serializer().serialize(record.topic(), record.key());
//    byte[] serializedValue = valueSerde.serializer().serialize(record.topic(), record.value());
//    log.info("Serialized Key: {}", serializedKey);
//    log.info("Serialized Value: {}", serializedValue);
//
//    byte[] updatedKey = Arrays.copyOfRange(serializedKey, headerBytes, serializedKey.length);
//    byte[] updatedValue = Arrays.copyOfRange(serializedValue, headerBytes, serializedValue.length);
//
//    record.headers().add(AVRO_KEY_HEADER, Arrays.copyOfRange(serializedKey, 0, headerBytes));
//    record.headers().add(AVRO_VALUE_HEADER, Arrays.copyOfRange(serializedValue, 0, headerBytes));
//
//    ProducerRecord<byte[], byte[]> newRecord = new ProducerRecord<>(record.topic(),
//        record.partition(), record.timestamp(), updatedKey,
//        updatedValue, record.headers());
//
//    log.info("Replacement Produce record: {}", newRecord);
//    log.info("   Key: {}", newRecord.key());
//    log.info("   Value: {}", newRecord.value());
//    log.info("   Headers: {}", newRecord.headers());
    return record;
  }

  @Override
  public void onAcknowledgement(RecordMetadata recordMetadata, Exception e) {

  }

  @Override
  public void close() {

  }

  @Override
  public void configure(Map<String, ?> map) {
    log.info("Configuring interceptor:");
    map.forEach((key, value) -> log.info("  {}: {}", key, value));
    keySerde.configure(Collections
        .singletonMap(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG,
            map.get("schema-registry-url")), true);
    valueSerde.configure(Collections
        .singletonMap(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG,
            map.get("schema-registry-url")), false);
  }
}
