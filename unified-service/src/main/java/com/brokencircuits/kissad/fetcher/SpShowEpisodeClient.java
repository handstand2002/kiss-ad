package com.brokencircuits.kissad.fetcher;

import com.brokencircuits.kissad.domain.fetcher.ShowEpisodeResponse;
import feign.Param;
import feign.RequestLine;

public interface SpShowEpisodeClient {
  @RequestLine("GET /api/?f=show&tz=America/New_York&sid={showId}")
  ShowEpisodeResponse findAll(@Param("showId") String showId);
}