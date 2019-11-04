package com.brokencircuits.kissad.util;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Objects;
import java.util.UUID;
import lombok.experimental.Delegate;

// CharSequence allows Avro serializer to convert this object to string
public class Uuid implements CharSequence, java.io.Serializable, Comparable<UUID> {

  @Delegate
  private UUID inner;

  public Uuid(long mostSigBits, long leastSigBits) {
    inner = new UUID(mostSigBits, leastSigBits);
  }

  @JsonCreator  // allows jackson to utilize this constructor to deserialize from JSON
  public Uuid(String uuid) {
    inner = UUID.fromString(uuid);
  }

  @Override
  public int length() {
    return inner.toString().length();
  }

  @Override
  public char charAt(int index) {
    return inner.toString().charAt(index);
  }

  @Override
  public CharSequence subSequence(int start, int end) {
    return inner.toString().subSequence(start, end);
  }

  @Override
  @JsonValue  // allows jackson to utilize this method to serialize to json
  public String toString() {
    return inner.toString();
  }

  @Override
  public boolean equals(Object obj) {
    return Objects.equals(inner, ((Uuid) obj).inner);
  }

  @Override
  public int hashCode() {
    return inner.hashCode();
  }

  public static Uuid randomUUID() {
    return new Uuid(UUID.randomUUID().toString());
  }

  public static Uuid nameUUIDFromBytes(byte[] name) {
    return new Uuid(UUID.nameUUIDFromBytes(name).toString());
  }


  public static Uuid fromString(String name) {
    return new Uuid(UUID.fromString(name).toString());
  }
}
