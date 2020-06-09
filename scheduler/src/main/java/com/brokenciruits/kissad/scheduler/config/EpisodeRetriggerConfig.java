package com.brokenciruits.kissad.scheduler.config;

import com.brokencircuits.kissad.kafka.AnonymousConsumer;
import com.brokencircuits.kissad.kafka.ByteKey;
import com.brokencircuits.kissad.kafka.KeyValueStoreWrapper;
import com.brokencircuits.kissad.messages.EpisodeMsg;
import com.brokencircuits.kissad.messages.EpisodeMsgKey;
import com.brokencircuits.kissad.util.Uuid;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class EpisodeRetriggerConfig {

  // TODO: check if the episodeQueueConsumer is starting up
  @Bean
  Map<Uuid, Instant> lastNewEpisodeTriggered(
      AnonymousConsumer<ByteKey<EpisodeMsgKey>, EpisodeMsg> episodeQueueConsumer,
      KeyValueStoreWrapper<ByteKey<EpisodeMsgKey>, EpisodeMsg> episodeStoreWrapper) {

    HashMap<Uuid, Instant> lastNewEpisodePerShow = new HashMap<>();
    episodeQueueConsumer.setMessageListener(
        newEpisode -> {
          log.info("New episode detected {}", newEpisode.value());
          if (!episodeStoreWrapper.isInitialized()) {
            return;
          }
          log.info("Episode store initialized");
          if (episodeStoreWrapper.get(newEpisode.key()) == null) {
            log.info("New episode detected for show: {}", newEpisode);
            lastNewEpisodePerShow
                .put(newEpisode.value().getKey().getShowId().getShowId(), Instant.now());
          }
        });
    return lastNewEpisodePerShow;
  }

}
