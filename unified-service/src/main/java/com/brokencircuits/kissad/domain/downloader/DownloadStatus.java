package com.brokencircuits.kissad.domain.downloader;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@AllArgsConstructor
public class DownloadStatus {

  String rootGid;
  String title;
  long completedLength;
  long connections;
  long downloadSpeed;
  List<FileStatus> files;
  long numPieces;
  long numSeeders;
  long totalLength;
  long uploadLength;
  long uploadSpeed;

  @Value
  @Builder(toBuilder = true)
  @AllArgsConstructor
  public static class FileStatus {
    long completedLength;
    long length;
    String path;
    int errorCode;
    String errorMessage;
    String status;
  }
}
