package com.brokencircuits.kissad.ui.fetcher.domain;

import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.Value;

@ToString
@Value
@RequiredArgsConstructor
public class MagnetUrl {
  int quality;
  String url;

  @ToString.Include(name = "url")
  public String urlString() {
    return url.substring(0, 20) + "...";
  }
}
