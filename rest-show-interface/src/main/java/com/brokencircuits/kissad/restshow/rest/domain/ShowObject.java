package com.brokencircuits.kissad.restshow.rest.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@JsonDeserialize(builder = ShowObject.ShowObjectBuilder.class)
public class ShowObject {

  @NonNull
  private String showUrl;
  @NonNull
  private String showName;
  private Integer seasonNumber;
  private Long showId;
  private Boolean isActive = true;
  private String initialSkipEpisodeString = null;

  @JsonPOJOBuilder(withPrefix = "")
  public static class ShowObjectBuilder {

  }

}
