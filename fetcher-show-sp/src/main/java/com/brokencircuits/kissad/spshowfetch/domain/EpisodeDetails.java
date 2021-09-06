package com.brokencircuits.kissad.spshowfetch.domain;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@ToString
@Data
@RequiredArgsConstructor
public class EpisodeDetails {
  private final long episodeNumber;
  private final List<MagnetUrl> urlList = new ArrayList<>();
}
