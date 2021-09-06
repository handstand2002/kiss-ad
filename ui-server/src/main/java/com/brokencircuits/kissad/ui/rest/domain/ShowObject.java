package com.brokencircuits.kissad.ui.rest.domain;

import com.brokencircuits.kissad.messages.SourceName;
import com.brokencircuits.kissad.ui.rest.domain.ShowObject.ShowObjectBuilder;
import com.brokencircuits.kissad.util.Uuid;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@JsonDeserialize(builder = ShowObjectBuilder.class)
public class ShowObject {

  @NonNull
  private String title;
  private Integer season;
  private Uuid showId;
  private Boolean isActive = true;
  private String initialSkipEpisodeString = null;
  private String releaseScheduleCron;
  private String url;
  private SourceName sourceName;
  private String episodeNamePattern = null;
  private String folderName;
  private String nextEpisode;

  @JsonPOJOBuilder(withPrefix = "")
  public static class ShowObjectBuilder {

  }

}
