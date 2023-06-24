package com.brokencircuits.kissad.domain;

import com.brokencircuits.kissad.domain.ShowDto.ShowDtoBuilder;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

@Entity
@Table(name = "show")
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder(toBuilder = true)
@JsonDeserialize(builder = ShowDtoBuilder.class)
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
  private String sourceName;
  @Column(name = "source")
  private String url;
  @Column(name = "active", columnDefinition = "boolean default true")
  @Nullable
  private Boolean isActive;
  @Transient
  private String nextEpisode;

  @JsonPOJOBuilder(withPrefix = "")
  public static class ShowDtoBuilder {

  }
}
