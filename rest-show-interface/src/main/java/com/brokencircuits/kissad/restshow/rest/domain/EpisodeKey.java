package com.brokencircuits.kissad.restshow.rest.domain;

import com.brokencircuits.kissad.messages.SubOrDub;
import com.brokencircuits.kissad.restshow.rest.domain.EpisodeKey.EpisodeKeyBuilder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@JsonDeserialize(builder = EpisodeKeyBuilder.class)
public class EpisodeKey {

  @NonNull
  private String showName;
  @NonNull
  private String episodeName;
  private Integer episodeNumber;
  private Integer seasonNumber;
  private SubOrDub subOrDub;

  @JsonPOJOBuilder(withPrefix = "")
  public static class EpisodeKeyBuilder {

  }
}