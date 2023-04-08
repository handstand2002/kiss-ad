package com.brokencircuits.kissad.domain.rest;

import com.brokencircuits.kissad.domain.rest.EpisodeObject.EpisodeObjectBuilder;
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