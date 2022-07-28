package com.brokencircuits.downloader.domain;

import com.brokencircuits.downloader.domain.download.DownloadResult;
import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@Value
@AllArgsConstructor
@Builder
public class AriaResponseStatus {

  String id;
  @SerializedName("jsonrpc")
  String rpcVersion;
  DownloadResult result;
}
