package com.brokencircuits.kissad.restshow.config;

import com.brokencircuits.kissad.kafka.KeyValueStore;
import com.brokencircuits.kissad.kafka.Publisher;
import com.brokencircuits.kissad.messages.KissShowMessage;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;

@Slf4j
@Configuration
public class ScheduleConfig {

  @Bean(name = "newEpisodePollSchedule")
  @ConfigurationProperties(prefix = "new-episode-poll")
  List<String> schedule() {
    return new ArrayList<>();
  }

  @Bean
  CommandLineRunner checkForNewEpisodesOnSchedule(TaskScheduler taskScheduler,
      List<String> newEpisodePollSchedule,
      KeyValueStore<Long, KissShowMessage> showMessageStore,
      Publisher<Long, KissShowMessage> showMessagePublisher) {

    // schedule all the cron strings
    return args -> newEpisodePollSchedule.forEach(
        scheduleString -> {
          log.info("Scheduling republish {}", scheduleString);
          taskScheduler.schedule(() -> {
            KeyValueIterator<Long, KissShowMessage> iter = showMessageStore.getStore().all();
            while (iter.hasNext()) {
              KeyValue<Long, KissShowMessage> entry = iter.next();
              if (entry.value.getIsActive()) {

                // this field is only used for initial publish
                entry.value.setSkipEpisodeString(null);
                showMessagePublisher.send(entry);
              }
            }
          }, new CronTrigger(scheduleString));
        });
  }

}
