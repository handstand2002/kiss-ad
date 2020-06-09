package com.brokencircuits.kissad.runtime;

import java.util.Collection;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AutostartRunner {

  @Autowired
  private final Collection<AutostartService> services;

  @PostConstruct
  private void startAll() throws Exception {
    for (AutostartService service : services) {
      service.start();
    }
  }
}
