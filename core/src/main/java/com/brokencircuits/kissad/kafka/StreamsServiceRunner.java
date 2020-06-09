package com.brokencircuits.kissad.kafka;

import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class StreamsServiceRunner {

  private final StreamsService service;

  @PostConstruct
  public void run() {
    log.info("Starting stream services...");
    service.start();
    Runtime.getRuntime().addShutdownHook(new Thread(service::stop));
  }
}
