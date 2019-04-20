package com.brokencircuits.kissad.kafka;

import java.util.Collection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class StreamsServiceRunner implements Runnable {

  private final Collection<StreamsService> services;

  @Override
  public void run() {
    log.info("Starting stream services...");
    services.forEach(StreamsService::start);
    Runtime.getRuntime().addShutdownHook(new Thread(() -> services.forEach(StreamsService::stop)));
  }
}
