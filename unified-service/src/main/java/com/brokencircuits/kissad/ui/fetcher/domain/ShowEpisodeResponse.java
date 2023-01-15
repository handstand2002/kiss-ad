package com.brokencircuits.kissad.ui.fetcher.domain;

import lombok.Data;

import java.util.Map;

@Data
public class ShowEpisodeResponse {
  private Map<String, SpEpisode> episode;
}
