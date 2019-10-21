package com.brokencircuits.kissad.kafka;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import lombok.Getter;

public class ClusterConnectionProps {

  @Getter
  private Map<String, String> clusterConnection = new HashMap<>();

  public Properties asProperties() {
    Properties props = new Properties();
    clusterConnection.forEach(props::put);
    return props;
  }
}
