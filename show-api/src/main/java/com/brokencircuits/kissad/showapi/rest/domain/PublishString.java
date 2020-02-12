package com.brokencircuits.kissad.showapi.rest.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@JsonDeserialize(builder = PublishString.ShowObjectBuilder.class)
public class PublishString {

  private String topic;
  private String key;
  private String value;

  @JsonPOJOBuilder(withPrefix = "")
  public static class ShowObjectBuilder {

  }

}
