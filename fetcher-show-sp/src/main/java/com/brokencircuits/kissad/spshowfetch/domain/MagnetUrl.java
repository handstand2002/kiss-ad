package com.brokencircuits.kissad.spshowfetch.domain;

import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.Value;

@ToString
@Value
@RequiredArgsConstructor
public class MagnetUrl {
  int quality;
  String url;
}
