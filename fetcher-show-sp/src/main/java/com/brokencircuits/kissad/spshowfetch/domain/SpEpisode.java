package com.brokencircuits.kissad.spshowfetch.domain;

import java.util.List;
import lombok.Data;

@Data
public class SpEpisode {
  private int episode;
  private String releaseDate;
  private String show;
  private List<SpEpisodeDownloadEntry> downloads;
}
