package com.brokencircuits.kissad.restshow.rest.domain;

import com.brokencircuits.kissad.messages.SourceName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@JsonDeserialize(builder = ShowSource.ShowSourceBuilder.class)
public class ShowSource {

  private SourceName sourceName;
  private String url;

  @JsonPOJOBuilder(withPrefix = "")
  public static class ShowSourceBuilder {

  }
}
