package com.brokencircuits.kissad.delegator.kafka;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConfigurationProperties(prefix = "messaging")
public class StreamProperties {

  @Getter
  private Map<String, String> streamConfig = new HashMap<>();

  /**
   * modified configs, so the properties can be directly used as arguments for producers, consumers,
   * streams
   */
  public Map<String, String> getUpdatedStreamConfig() {
    Map<String, String> updatedConfig = new HashMap<>();
    streamConfig.forEach((key, value) -> updatedConfig.put(key.replace("-", "."), value));
    return updatedConfig;
  }
}
