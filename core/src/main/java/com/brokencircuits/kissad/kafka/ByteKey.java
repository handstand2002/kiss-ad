package com.brokencircuits.kissad.kafka;

import com.apple.foundationdb.Range;
import com.apple.foundationdb.tuple.Tuple;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.streams.KeyValue;

@Slf4j
@EqualsAndHashCode
public class ByteKey<T extends SpecificRecordBase> {

  private final byte[] innerBytes;

  public static <T extends SpecificRecordBase> ByteKey<T> from(T inner) {
    return new ByteKey<>(inner);
  }

  public static <U extends SpecificRecordBase> KeyValue<ByteKey<U>, ByteKey<U>> rangeFrom(
      Object... fieldValues) {
    Range range = Tuple.from(fieldValues).range();
    return KeyValue.pair(new ByteKey<>(range.begin), new ByteKey<>(range.end));
  }

  public ByteKey(T inner) {
    Objects.requireNonNull(inner);

    Schema schema = inner.getSchema();
    List<Field> fields = new ArrayList<>(schema.getFields());

    Tuple tuple = new Tuple();
    for (Field field : fields) {
      Object fieldValue = inner.get(field.pos());
      if (fieldValue != null) {
        try {
          tuple = tuple.addObject(fieldValue);
        } catch (IllegalArgumentException e) {
          tuple = tuple.add(fieldValue.toString());
        }
      }
    }
    innerBytes = tuple.pack();
  }

  public ByteKey(byte[] innerBytes) {
    this.innerBytes = innerBytes;
  }

  public byte[] getBytes() {
    return innerBytes;
  }

  public String toString() {
    return "ByteKey" + Tuple.fromBytes(innerBytes);
  }
}
