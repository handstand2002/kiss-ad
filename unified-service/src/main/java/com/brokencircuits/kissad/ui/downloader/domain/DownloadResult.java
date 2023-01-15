package com.brokencircuits.kissad.ui.downloader.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@AllArgsConstructor
public class DownloadResult {

  long completedLength;
  long connections;
  String dir;
  long downloadSpeed;
  int errorCode;
  String errorMessage;
  List<FileDetails> files;
  List<String> followedBy;
  String gid;
  String infoHash;
  long numPieces;
  long numSeeders;
  long pieceLength;
  String status;
  long totalLength;
  long uploadLength;
  long uploadSpeed;
}
