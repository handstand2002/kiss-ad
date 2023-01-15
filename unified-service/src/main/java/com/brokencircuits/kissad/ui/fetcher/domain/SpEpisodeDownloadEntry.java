package com.brokencircuits.kissad.ui.fetcher.domain;

import lombok.Data;

@Data
public class SpEpisodeDownloadEntry {
  private String magnet;
  private int res;
  private String torrent;
  private String xdcc;
}
