package com.brokencircuits.kissad.novaepisodefetch.config;

import com.brokencircuits.kissad.config.TopicAutoconfig;
import com.brokencircuits.kissad.kafka.ByteKey;
import com.brokencircuits.kissad.kafka.ClusterConnectionProps;
import com.brokencircuits.kissad.kafka.Publisher;
import com.brokencircuits.kissad.kafka.Topic;
import com.brokencircuits.kissad.messages.EpisodeMsg;
import com.brokencircuits.kissad.messages.EpisodeMsgKey;
import com.brokencircuits.kissad.messages.KissEpisodeExternalSrcMsg;
import com.brokencircuits.kissad.novaepisodefetch.nova.NovaApiClient;
import com.brokencircuits.kissad.novaepisodefetch.streams.EpisodeProcessor;
import org.apache.kafka.streams.processor.ProcessorSupplier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({TopicAutoconfig.class})
public class KafkaConfig {

  @Bean
  Publisher<ByteKey<EpisodeMsgKey>, EpisodeMsg> episodeMsgPublisher(ClusterConnectionProps props,
      Topic<ByteKey<EpisodeMsgKey>, EpisodeMsg> episodeQueueTopic) {
    return new Publisher<>(props.asProperties(), episodeQueueTopic);
  }

  @Bean
  ProcessorSupplier<ByteKey<EpisodeMsgKey>, KissEpisodeExternalSrcMsg> episodeProcessorSupplier(
      NovaApiClient novaApiClient,
      Publisher<ByteKey<EpisodeMsgKey>, EpisodeMsg> episodeMsgPublisher) {
    return () -> new EpisodeProcessor(novaApiClient, episodeMsgPublisher);
  }

//  @Bean
//  CommandLineRunner testing(
//      ProcessorSupplier<ByteKey<EpisodeMsgKey>, KissEpisodeExternalSrcMsg> episodeProcessorSupplier) {
//    return args -> {
//      Processor<ByteKey<EpisodeMsgKey>, KissEpisodeExternalSrcMsg> processor = episodeProcessorSupplier
//          .get();
//
//      EpisodeMsgKey key = EpisodeMsgKey.newBuilder()
//          .setEpisodeNumber(1L)
//          .setShowId(ShowMsgKey.newBuilder().setShowId(Uuid.randomUUID()).build())
//          .setRawTitle("Something")
//          .build();
//
//      KissEpisodeExternalSrcMsg msg = KissEpisodeExternalSrcMsg.newBuilder()
//          .setEpisodeMsgKey(key)
//          .setSource(ExternalSources.NOVA)
//          .setUrl("https://www.novelplanet.me/v/ln-11cnngqy12qk")
//          .build();
//
//      processor.process(new ByteKey<>(key), msg);
//    };
//  }
}
