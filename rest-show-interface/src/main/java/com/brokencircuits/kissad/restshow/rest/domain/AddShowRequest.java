package com.brokencircuits.kissad.restshow.rest.domain;

import lombok.Value;

@Value
public class AddShowRequest {

  private String showUrl;
  private String showName;
  private Long showId;
}
