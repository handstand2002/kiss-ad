package com.brokencircuits.kissad.download.domain;

import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@ToString
@Getter
@Setter(value = AccessLevel.PACKAGE)
@RequiredArgsConstructor
public class DownloadStatus {

  final private UUID downloadId;
  final private String uri;
  final private String destinationDir;
  final private String destinationFileName;
  final private DownloadType downloadType;

  private final Instant createTime = Instant.now();
  private long downloaderId;
  private boolean hasStarted = false;
  private boolean isFinished = false;
  private String resolvedDestinationPath;
  private Instant startTime;
  private Instant endTime;
  private Instant lastUpdate;
  private long bytesPerSecond;
  private long bytesDownloaded;
  private long bytesTotal;
  private long connections;
  private int errorCode = 0;
  private String errorMessage = null;
  private long numPieces;
  private long pieceLength;
  private long numSeeders;
  private String status;
}
