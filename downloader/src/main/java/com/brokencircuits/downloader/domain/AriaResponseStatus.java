package com.brokencircuits.downloader.domain;

import com.brokencircuits.downloader.domain.download.DownloadResult;
import com.google.gson.annotations.SerializedName;
import lombok.Value;

@Value
public class AriaResponseStatus {

  private String id;
  @SerializedName("jsonrpc")
  private String rpcVersion;
  DownloadResult result;
}
