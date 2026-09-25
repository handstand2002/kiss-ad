package com.brokencircuits.kissad.domain.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EpDeleteMsg {
  private String showId;
  private int episodeNumber;
}
