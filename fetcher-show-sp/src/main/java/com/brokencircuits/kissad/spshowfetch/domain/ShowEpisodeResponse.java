package com.brokencircuits.kissad.spshowfetch.domain;

import java.util.Map;
import lombok.Data;

@Data
public class ShowEpisodeResponse {
  private Map<String, SpEpisode> episode;
}
