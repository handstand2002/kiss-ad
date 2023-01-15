package com.brokencircuits.kissad.ui.fetcher;

import com.brokencircuits.kissad.ui.fetcher.domain.ShowEpisodeResponse;
import feign.Param;
import feign.RequestLine;

public interface SpShowEpisodeClient {
  @RequestLine("GET /api/?f=show&tz=America/New_York&sid={showId}")
  ShowEpisodeResponse findAll(@Param("showId") String showId);
}