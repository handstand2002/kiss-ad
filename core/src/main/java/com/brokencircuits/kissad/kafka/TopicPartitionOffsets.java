package com.brokencircuits.kissad.kafka;

import lombok.RequiredArgsConstructor;
import lombok.Value;

@RequiredArgsConstructor
@Value
public class TopicPartitionOffsets {
  private String topic;
  private int partition;
  private long beginningOffset;
  private long endOffset;
}
