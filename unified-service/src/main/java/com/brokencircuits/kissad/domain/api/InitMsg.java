package com.brokencircuits.kissad.domain.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
public class InitMsg implements ServerMsg {
  private UiPage page;
  private Map<String, Object> params;
}
