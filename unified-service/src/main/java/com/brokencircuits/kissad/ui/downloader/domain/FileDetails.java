package com.brokencircuits.kissad.ui.downloader.domain;

import lombok.Value;

@Value
public class FileDetails {

  String completedLength;
  int index;
  long length;
  String path;
  boolean selected;
}

