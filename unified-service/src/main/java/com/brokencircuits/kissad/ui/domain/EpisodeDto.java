package com.brokencircuits.kissad.ui.domain;


import java.time.Instant;
import java.util.List;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "episode")
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
@IdClass(EpisodeId.class)
public class EpisodeDto {

  @Id
  private String showId;
  @Id
  private int episodeNumber;
  @Column(name = "downloadTime")
  private Instant downloadTime;
  @Column(name = "downloadedQuality")
  private int downloadedQuality;
  @Column(name = "latestLinks")
  @ElementCollection
  @CollectionTable(name = "links")
  private List<EpisodeLinkDto> latestLinks;
}
