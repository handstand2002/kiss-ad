package com.brokencircuits.kissad.domain;

import lombok.Value;

@Value
public class RequestEpisodeResult {

  boolean downloadComplete;
  boolean error;
}
