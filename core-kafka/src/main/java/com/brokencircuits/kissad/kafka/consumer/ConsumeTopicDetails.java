package com.brokencircuits.kissad.kafka.consumer;

import com.brokencircuits.kissad.kafka.Topic;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ConsumeTopicDetails<K,V> {
  Topic<K,V> topic;
  /**
   * Action to take when processing record, before deserializing it. This is always run before {@link #deserializedAction}
   */
  RecordProcessAction<byte[],byte[]> serializedAction;
  /**
   * Action to take when processing record, after deserializing it. This is always run after {@link #serializedAction}
   */
  RecordProcessAction<K,V> deserializedAction;
}
