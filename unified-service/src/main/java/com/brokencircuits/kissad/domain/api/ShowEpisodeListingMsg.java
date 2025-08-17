package com.brokencircuits.kissad.domain.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ShowEpisodeListingMsg implements UiMsg {

  private String downloadTime;
  private int downloadedQuality;
  private int episodeNumber;
  private boolean isDelete;
}
