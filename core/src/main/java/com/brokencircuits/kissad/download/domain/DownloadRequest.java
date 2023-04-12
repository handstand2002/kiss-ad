package com.brokencircuits.kissad.download.domain;

import java.util.UUID;
import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class DownloadRequest {

  DownloadType type;
  String destinationDir;
  String destinationFileName;
  UUID downloadId;
  String uri;
}
