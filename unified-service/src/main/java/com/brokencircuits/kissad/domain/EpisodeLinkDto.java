package com.brokencircuits.kissad.domain;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class EpisodeLinkDto {

  private int quality;
  private DownloadTypeDto type;
  @Column(length = 2048)
  private String url;
}
