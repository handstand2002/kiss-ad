package com.brokencircuits.downloader.domain;

import com.google.gson.annotations.SerializedName;
import lombok.Value;

@Value
public class AriaResponseUriSubmit {

  private String id;

  @SerializedName("jsonrpc")
  private String rpcVersion;
  @SerializedName("result")
  private String gid;
}
