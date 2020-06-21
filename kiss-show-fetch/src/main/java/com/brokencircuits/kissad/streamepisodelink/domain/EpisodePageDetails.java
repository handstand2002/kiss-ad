package com.brokencircuits.kissad.streamepisodelink.domain;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EpisodePageDetails {

  private String episodeUrl;
  private Integer episodeNumber;
  private String episodeName;
}
