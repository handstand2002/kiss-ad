package com.brokencircuits.kissad.showapi.rest.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.util.Collection;
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
  private String title;
  private Integer season;
  private Long showId;
  private Boolean isActive = true;
  private String initialSkipEpisodeString = null;
  private Collection<ShowSource> sources;

  @JsonPOJOBuilder(withPrefix = "")
  public static class ShowObjectBuilder {

  }

}
