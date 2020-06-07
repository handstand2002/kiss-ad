package com.brokencircuits.kissad.runtime;

import com.brokencircuits.kissad.util.Service;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;

@RequiredArgsConstructor
public class AutostartServiceWrapper implements AutostartService {

  @Delegate
  private final Service inner;
}
