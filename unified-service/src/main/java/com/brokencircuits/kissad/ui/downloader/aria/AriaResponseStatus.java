package com.brokencircuits.kissad.ui.downloader.aria;

import com.brokencircuits.kissad.ui.downloader.domain.DownloadResult;
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
