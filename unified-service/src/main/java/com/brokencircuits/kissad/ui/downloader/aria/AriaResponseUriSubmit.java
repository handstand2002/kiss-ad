package com.brokencircuits.kissad.ui.downloader.aria;

import com.google.gson.annotations.SerializedName;
import lombok.Value;

@Value
public class AriaResponseUriSubmit {

  String id;

  @SerializedName("jsonrpc")
  String rpcVersion;
  @SerializedName("result")
  String gid;
}
