package com.brokencircuits.kissad.domain.fetcher;

import lombok.Data;

import java.util.List;

@Data
public class SpEpisode {
  private String episode;
  private String releaseDate;
  private String show;
  private List<SpEpisodeDownloadEntry> downloads;
}
