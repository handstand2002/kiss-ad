package com.brokencircuits.kissad.ui.domain;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EpisodeId implements Serializable {

  private String showId;

  private int episodeNumber;
}