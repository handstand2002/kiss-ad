package com.brokencircuits.kissad.novaepisodefetch.domain;

import java.util.Collection;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class NovaShowDetails {

  private Collection<FileDetails> data;
}
