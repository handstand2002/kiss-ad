package com.brokencircuits.kissad.domain;

import java.util.UUID;

public interface CheckShowOperation {
  CheckShowResult run(UUID showId);

}
