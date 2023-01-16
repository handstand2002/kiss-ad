package com.brokencircuits.kissad.kafka.config;

import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "kafka")
@Data
public class InternalKafkaConfig {

  private Map<String, String> global = new HashMap<>();
  private Map<String, String> consumer = new HashMap<>();
  private Map<String, String> producer = new HashMap<>();
  private Map<String, String> streams = new HashMap<>();
  private Map<String, String> admin = new HashMap<>();
}
