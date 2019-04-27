package com.brokencircuits.kissad.restshow.rest.domain;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ShowObject {

  private String showUrl;
  private String showName;
  private Integer seasonNumber;
  private Long showId;
  private Boolean isActive = true;
  private String initialSkipEpisodeString = null;
}
