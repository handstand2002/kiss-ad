package com.brokencircuits.kissad.streamepdownload.config;

import com.brokencircuits.kissad.kafka.Publisher;
import com.brokencircuits.kissad.kafka.StreamsService;
import com.brokencircuits.kissad.kafka.StreamsServiceRunner;
import com.brokencircuits.kissad.messages.DownloadAvailability;
import java.time.Instant;
import java.util.Collection;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;

@Configuration
public class RuntimeConfig {

  @Bean
  Future<RecordMetadata> sendAvailabilityUpdate(
      @Value("${messaging.application-id}") String applicationId,
      Publisher<String, DownloadAvailability> availabilityPublisher) {
    return availabilityPublisher.send(applicationId,
        DownloadAvailability.newBuilder().setAvailableCapacity(1).build());
  }

  @Bean
  ScheduledFuture<?> startStreams(TaskScheduler taskScheduler, StreamsServiceRunner serviceRunner,
      @Value("${messaging.application-id}") String applicationId,
      Publisher<String, DownloadAvailability> availabilityPublisher) {
    ScheduledFuture<?> schedule = taskScheduler
        .schedule(serviceRunner, Instant.now().plusSeconds(10));
    Runtime.getRuntime().addShutdownHook(new Thread(
        () -> {
          schedule.cancel(true);
          availabilityPublisher.send(applicationId,
              DownloadAvailability.newBuilder().setAvailableCapacity(0).build());
        }));
    return schedule;
  }

  @Bean
  StreamsServiceRunner serviceRunner(Collection<StreamsService> services) {
    return new StreamsServiceRunner(services);
  }
}
