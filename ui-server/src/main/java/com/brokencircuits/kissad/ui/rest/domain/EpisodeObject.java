package com.brokencircuits.kissad.ui.rest.domain;

import com.brokencircuits.kissad.ui.rest.domain.EpisodeObject.EpisodeObjectBuilder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@JsonDeserialize(builder = EpisodeObjectBuilder.class)
public class EpisodeObject {

  private String downloadTime;
  private int downloadedQuality;
  private Long episodeNumber;

  @JsonPOJOBuilder(withPrefix = "")
  public static class EpisodeObjectBuilder {

  }

}