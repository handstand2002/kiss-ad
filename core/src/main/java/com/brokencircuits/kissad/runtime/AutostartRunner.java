package com.brokencircuits.kissad.runtime;

import java.util.Collection;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AutostartRunner {

  @Autowired(required = false)
  private final Collection<AutostartService> services;

  @PostConstruct
  private void startAll() throws Exception {
    if (services == null) {
      return;
    }
    for (AutostartService service : services) {
      service.start();
    }
  }
}
