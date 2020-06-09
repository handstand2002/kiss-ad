package com.brokencircuits.kissad.kafka;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import lombok.Data;
import lombok.Getter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnMissingBean(ClusterConnectionProps.class)
@ConfigurationProperties(prefix = "messaging")
@Data
public class ClusterConnectionProps {

  @Getter
  private final Map<String, String> clusterConnection = new HashMap<>();
  private String schemaRegistryUrl;

  public Properties asProperties() {
    Properties props = new Properties();
    clusterConnection.forEach(props::put);
    return props;
  }

  public Map<String, Object> asObjectMap() {
    Map<String, Object> outputMap = new HashMap<>();
    clusterConnection.forEach(outputMap::put);
    return outputMap;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    clusterConnection.forEach((key, value) -> sb.append("  ").append(key).append(": ").append(value)
        .append("\n"));
    return sb.toString();
  }
}
