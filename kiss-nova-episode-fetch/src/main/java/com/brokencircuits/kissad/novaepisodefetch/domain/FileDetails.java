package com.brokencircuits.kissad.novaepisodefetch.domain;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class FileDetails {

  private String file;
  private String label;
  private String type;
}
