package com.brokencircuits.kissad.streamepisodelink.config;

import com.brokencircuits.kissad.kafka.StreamsService;
import com.brokencircuits.kissad.kafka.StreamsServiceRunner;
import java.time.Instant;
import java.util.Collection;
import java.util.concurrent.ScheduledFuture;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;

@Configuration
public class RuntimeConfig {

  @Bean
  ScheduledFuture<?> startStreams(TaskScheduler taskScheduler, StreamsServiceRunner serviceRunner) {
    ScheduledFuture<?> schedule = taskScheduler.schedule(serviceRunner, Instant.now());
    Runtime.getRuntime().addShutdownHook(new Thread(
        () -> schedule.cancel(true)));
    return schedule;
  }

  @Bean
  StreamsServiceRunner serviceRunner(Collection<StreamsService> services) {
    return new StreamsServiceRunner(services);
  }
}
