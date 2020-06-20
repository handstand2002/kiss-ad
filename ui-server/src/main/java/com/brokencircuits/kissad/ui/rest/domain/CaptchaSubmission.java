package com.brokencircuits.kissad.ui.rest.domain;

import com.brokencircuits.kissad.ui.rest.domain.CaptchaSubmission.CaptchaSubmissionBuilder;
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
@JsonDeserialize(builder = CaptchaSubmissionBuilder.class)
public class CaptchaSubmission {

  @NonNull
  private String hash;
  @NonNull
  private String keyword;
  @NonNull
  private Uuid batchId;

  @JsonPOJOBuilder(withPrefix = "")
  public static class CaptchaSubmissionBuilder {

  }
}
