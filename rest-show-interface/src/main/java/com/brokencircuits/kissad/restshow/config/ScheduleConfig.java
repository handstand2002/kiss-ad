package com.brokencircuits.kissad.restshow.config;

import com.brokencircuits.kissad.kafka.KeyValueStore;
import com.brokencircuits.kissad.kafka.Publisher;
import com.brokencircuits.kissad.messages.KissShowMessage;
import java.util.concurrent.ScheduledFuture;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;

@Configuration
public class ScheduleConfig {

  @Bean
  ScheduledFuture<?> checkForNewEpisodesOnSchedule(TaskScheduler taskScheduler,
      @Value("${new-episode-poll.schedule}") String cronSchedule,
      KeyValueStore<Long, KissShowMessage> showMessageStore,
      Publisher<Long, KissShowMessage> showMessagePublisher) {

    return taskScheduler.schedule(() -> {
      KeyValueIterator<Long, KissShowMessage> iter = showMessageStore.getStore().all();
      while (iter.hasNext()) {
        KeyValue<Long, KissShowMessage> entry = iter.next();
        if (entry.value.getIsActive()) {
          showMessagePublisher.send(entry);
        }
      }
    }, new CronTrigger(cronSchedule));
  }

}
