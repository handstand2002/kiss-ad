package com.brokencircuits.kissad.hsshowfetch.domain;

import avro.shaded.com.google.common.collect.Lists;
import java.util.List;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@ToString
@Data
@RequiredArgsConstructor
public class EpisodeDetails {
  private final long episodeNumber;
  private final List<MagnetUrl> urlList = Lists.newArrayList();
}
