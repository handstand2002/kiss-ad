package com.brokencircuits.kissad.download.domain;

import java.time.Instant;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@ToString
@RequiredArgsConstructor
public class DownloadStatusWrapper {

  @Getter
  private final DownloadStatus status;

  public DownloadStatusWrapper setDownloaderId(long downloaderId) {
    status.setDownloaderId(downloaderId);
    return this;
  }

  public DownloadStatusWrapper setHasStarted(boolean hasStarted) {
    status.setHasStarted(hasStarted);
    return this;
  }

  public DownloadStatusWrapper setIsFinished(boolean isFinished) {
    status.setFinished(isFinished);
    return this;
  }

  public DownloadStatusWrapper setResolvedDestinationPath(String resolvedDestinationPath) {
    status.setResolvedDestinationPath(resolvedDestinationPath);
    return this;
  }

  public DownloadStatusWrapper setStartTime(Instant startTime) {
    status.setStartTime(startTime);
    return this;
  }

  public DownloadStatusWrapper setEndTime(Instant endTime) {
    status.setEndTime(endTime);
    return this;
  }

  public DownloadStatusWrapper setLastUpdate(Instant lastUpdate) {
    status.setLastUpdate(lastUpdate);
    return this;
  }

  public DownloadStatusWrapper setBytesPerSecond(long bytesPerSecond) {
    status.setBytesPerSecond(bytesPerSecond);
    return this;
  }

  public DownloadStatusWrapper setBytesDownloaded(long bytesDownloaded) {
    status.setBytesDownloaded(bytesDownloaded);
    return this;
  }

  public DownloadStatusWrapper setBytesTotal(long bytesTotal) {
    status.setBytesTotal(bytesTotal);
    return this;
  }

  public DownloadStatusWrapper setConnections(long connections) {
    status.setConnections(connections);
    return this;
  }

  public DownloadStatusWrapper setErrorCode(int errorCode) {
    status.setErrorCode(errorCode);
    return this;
  }

  public DownloadStatusWrapper setErrorMessage(String errorMessage) {
    status.setErrorMessage(errorMessage);
    return this;
  }

  public DownloadStatusWrapper setNumPieces(long numPieces) {
    status.setNumPieces(numPieces);
    return this;
  }

  public DownloadStatusWrapper setPieceLength(long pieceLength) {
    status.setPieceLength(pieceLength);
    return this;
  }

  public DownloadStatusWrapper setNumSeeders(long numSeeders) {
    status.setNumSeeders(numSeeders);
    return this;
  }

  public DownloadStatusWrapper setStatus(String statusUpdate) {
    status.setStatus(statusUpdate);
    return this;
  }
}
