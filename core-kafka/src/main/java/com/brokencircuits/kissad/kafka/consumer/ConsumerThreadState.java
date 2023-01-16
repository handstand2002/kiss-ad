package com.brokencircuits.kissad.kafka.consumer;

public enum ConsumerThreadState {
  CREATED, STARTING, RUNNING, PENDING_SHUTDOWN, SHUTDOWN
}
