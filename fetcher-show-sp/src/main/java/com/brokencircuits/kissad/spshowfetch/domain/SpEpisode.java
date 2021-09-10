package com.brokencircuits.kissad.spshowfetch.domain;

import java.util.List;
import lombok.Data;

@Data
public class SpEpisode {
  private String episode;
  private String releaseDate;
  private String show;
  private List<SpEpisodeDownloadEntry> downloads;
}
