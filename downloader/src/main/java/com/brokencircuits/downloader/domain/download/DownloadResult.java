package com.brokencircuits.downloader.domain.download;

import java.util.List;
import lombok.Value;

@Value
public class DownloadResult {

  //  @SerializedName("bittorrent")
//  private BitTorrent bitTorrent;
  private long completedLength;
  private long connections;
  private String dir;
  private long downloadSpeed;
  private int errorCode;
  private String errorMessage;
  //  private List<FileDetails> files;
  private List<String> followedBy;
  private String gid;
  private String infoHash;
  private long numPieces;
  private long numSeeders;
  private long pieceLength;
  private String status;
  private long totalLength;
  private long uploadLength;
  private long uploadSpeed;
}
