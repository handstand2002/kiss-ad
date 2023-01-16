package com.brokencircuits.kissad.ui.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "show")
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class ShowDto {

  @Id
  private String id;
  @Column(name = "title")
  private String title;
  @Column(name = "season")
  private int season;
  @Column(name = "releaseScheduleCron")
  private String releaseScheduleCron;
  @Column(name = "skipEpisodeString")
  private String skipEpisodeString;
  @Column(name = "episodeNamePattern")
  private String episodeNamePattern;
  @Column(name = "folderName")
  private String folderName;
  @Column(name = "sourceType")
  private String sourceType;
  @Column(name = "source")
  private String source;
}
