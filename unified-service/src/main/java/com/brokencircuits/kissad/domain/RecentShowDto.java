package com.brokencircuits.kissad.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecentShowDto {

  private String downloaded;
  private String name;
}
