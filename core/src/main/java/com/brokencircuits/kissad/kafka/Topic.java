package com.brokencircuits.kissad.kafka;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.apache.kafka.common.serialization.Serde;

@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class Topic<K, V> extends SerdePair<K,V>{

  final private String name;

  @java.beans.ConstructorProperties({"name"})
  public Topic(String name, Serde<K> keySerde, Serde<V> valueSerde) {
    super(keySerde, valueSerde);
    this.name = name;
  }

  public String getName() {
    return this.name;
  }

}
