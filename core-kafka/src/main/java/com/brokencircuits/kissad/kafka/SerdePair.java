package com.brokencircuits.kissad.kafka;

import java.util.Objects;
import org.apache.kafka.common.serialization.Serde;

public class SerdePair<K, V> {

  private final Serde<K> keySerde;
  private final Serde<V> valueSerde;

  @java.beans.ConstructorProperties({"keySerde", "valueSerde"})
  public SerdePair(Serde<K> keySerde, Serde<V> valueSerde) {
    this.keySerde = keySerde;
    this.valueSerde = valueSerde;
  }

  public Serde<K> getKeySerde() {
    return this.keySerde;
  }

  public Serde<V> getValueSerde() {
    return this.valueSerde;
  }

  public boolean equals(final Object o) {
    if (o == this) {
      return true;
    }
    if (!(o instanceof SerdePair)) {
      return false;
    }
    final SerdePair<?, ?> other = (SerdePair<?, ?>) o;
    final Object this$keySerde = this.getKeySerde();
    final Object other$keySerde = other.getKeySerde();
    if (!Objects.equals(this$keySerde, other$keySerde)) {
      return false;
    }
    final Object this$valueSerde = this.getValueSerde();
    final Object other$valueSerde = other.getValueSerde();
    return Objects.equals(this$valueSerde, other$valueSerde);
  }

  public int hashCode() {
    final int PRIME = 59;
    int result = 1;
    final Object $keySerde = this.getKeySerde();
    result = result * PRIME + ($keySerde == null ? 43 : $keySerde.hashCode());
    final Object $valueSerde = this.getValueSerde();
    result = result * PRIME + ($valueSerde == null ? 43 : $valueSerde.hashCode());
    return result;
  }

  public String toString() {
    return "SerdePair(keySerde=" + this.getKeySerde() + ", valueSerde=" + this.getValueSerde()
        + ")";
  }
}
