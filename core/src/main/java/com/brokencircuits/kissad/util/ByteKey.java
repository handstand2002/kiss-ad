package com.brokencircuits.kissad.util;

import com.apple.foundationdb.tuple.Tuple;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.apache.avro.specific.SpecificRecordBase;

@Slf4j
@EqualsAndHashCode
public class ByteKey<T extends SpecificRecordBase> {

  @Getter
  private final byte[] innerBytes;
  @Getter
  private final T inner;

  public static <T extends SpecificRecordBase> ByteKey<T> from(T inner) {
    return new ByteKey<>(inner);
  }

  public ByteKey(T inner) {
    Objects.requireNonNull(inner);

    Schema schema = inner.getSchema();
    List<Field> fields = new ArrayList<>(schema.getFields());
    fields.sort(Comparator.comparing(Field::name));

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
    this.inner = inner;
  }

  public ByteKey(byte[] innerBytes) {
    this.innerBytes = innerBytes;
    this.inner = null;
  }

  public byte[] getBytes() {
    return innerBytes;
  }

  public String toString() {
    return "ByteKey" + Tuple.fromBytes(innerBytes);
  }
}