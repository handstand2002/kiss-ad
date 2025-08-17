package com.brokencircuits.kissad.domain.api;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ShowUpdateMsg implements ServerMsg {

  private ShowListingMsg details;
}
