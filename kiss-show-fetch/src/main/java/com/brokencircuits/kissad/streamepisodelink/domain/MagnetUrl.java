package com.brokencircuits.kissad.streamepisodelink.domain;

import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.Value;

@ToString
@Value
@RequiredArgsConstructor
public class MagnetUrl {

  private final int quality;
  private final String url;
}
