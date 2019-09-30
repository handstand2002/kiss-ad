package com.brokencircuits.downloader.domain;

import lombok.Value;

@Value
public class FileDetails {

  private String completedLength;
  private int index;
  private long length;
  private String path;
  private boolean selected;
}

