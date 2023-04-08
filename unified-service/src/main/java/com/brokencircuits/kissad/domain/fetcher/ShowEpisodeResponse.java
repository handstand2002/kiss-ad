package com.brokencircuits.kissad.domain.fetcher;

import lombok.Data;

import java.util.Map;

@Data
public class ShowEpisodeResponse {
  private Map<String, SpEpisode> episode;
}
