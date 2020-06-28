package com.brokencircuits.kissad.novaepisodefetch.nova;

import com.brokencircuits.kissad.novaepisodefetch.domain.NovaShowDetails;
import feign.Headers;
import feign.Param;
import feign.RequestLine;

public interface NovaApiClient {

  @RequestLine("POST /{identifier}")
  @Headers("Content-Type: application/json")
  NovaShowDetails create(@Param("identifier") String id);
}
