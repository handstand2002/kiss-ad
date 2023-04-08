package com.brokencircuits.kissad.domain.fetcher;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@ToString
@Data
@RequiredArgsConstructor
public class EpisodeDetails {
  private final long episodeNumber;
  private final List<MagnetUrl> urlList = new ArrayList<>();
}
