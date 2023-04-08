package com.brokencircuits.kissad.domain.downloader;

import lombok.Value;

@Value
public class FileDetails {

  String completedLength;
  int index;
  long length;
  String path;
  boolean selected;
}

