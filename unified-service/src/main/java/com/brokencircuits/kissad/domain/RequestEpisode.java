package com.brokencircuits.kissad.domain;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RequestEpisode {

  String showId;
  int episodeNumber;
  List<EpisodeLinkDto> links;
}
