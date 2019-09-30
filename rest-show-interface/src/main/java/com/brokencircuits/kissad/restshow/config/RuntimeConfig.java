package com.brokencircuits.kissad.restshow.config;

import com.brokencircuits.kissad.kafka.StreamsService;
import com.brokencircuits.kissad.kafka.StreamsServiceRunner;
import java.time.Instant;
import java.util.Collection;
import java.util.concurrent.ScheduledFuture;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;

@Configuration
public class RuntimeConfig {

  @Bean
  CommandLineRunner startStreams(StreamsServiceRunner serviceRunner) {
    return args -> serviceRunner.run();
  }

  @Bean
  StreamsServiceRunner serviceRunner(Collection<StreamsService> services) {
    return new StreamsServiceRunner(services);
  }

}
