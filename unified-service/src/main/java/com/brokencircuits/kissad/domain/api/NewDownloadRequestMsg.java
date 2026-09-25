package com.brokencircuits.kissad.domain.api;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class NewDownloadRequestMsg {
  private String url;
  private String destination;
}
