package com.brokencircuits.kissad.ui.fetcher.domain;

import lombok.Data;

import java.util.List;

@Data
public class SpEpisode {
  private String episode;
  private String releaseDate;
  private String show;
  private List<SpEpisodeDownloadEntry> downloads;
}
