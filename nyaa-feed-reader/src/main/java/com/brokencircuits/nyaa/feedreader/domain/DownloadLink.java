package com.brokencircuits.nyaa.feedreader.domain;

import java.time.ZonedDateTime;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DownloadLink {

  String title;
  String link;
  String guid;
  ZonedDateTime pubDate;
  int seeders;
  int leechers;
  int downloads;
  String infoHash;
  String categoryId;
  String category;
  String size;
  long comments;
  boolean trusted;
  boolean remake;
}
