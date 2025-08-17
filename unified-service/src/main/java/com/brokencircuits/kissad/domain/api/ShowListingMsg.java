package com.brokencircuits.kissad.domain.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ShowListingMsg implements UiMsg {

  private String id;
  private String title;
  private int season;
  private String releaseScheduleCron;
  private String skipEpisodeString;
  private String episodeNamePattern;
  private String folderName;
  private String sourceName;
  private String url;
  private Boolean isActive;
  private String nextEpisode;
}
