package com.brokencircuits.kissad.domain.internal;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DownloadStatusUpdatedEvent {
  String filename;
  float pcntComplete;
  long bytesPerSec;
  boolean isComplete;

}
