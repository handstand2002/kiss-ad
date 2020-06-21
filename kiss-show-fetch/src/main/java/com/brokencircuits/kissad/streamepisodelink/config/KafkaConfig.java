package com.brokencircuits.kissad.streamepisodelink.config;

import com.brokencircuits.kissad.Extractor;
import com.brokencircuits.kissad.config.TopicAutoconfig;
import com.brokencircuits.kissad.kafka.ByteKey;
import com.brokencircuits.kissad.kafka.Publisher;
import com.brokencircuits.kissad.kissweb.KissWebFetcher;
import com.brokencircuits.kissad.messages.KissEpisodePageKey;
import com.brokencircuits.kissad.messages.KissEpisodePageMessage;
import com.brokencircuits.kissad.messages.ShowMsg;
import com.brokencircuits.kissad.messages.ShowMsgKey;
import com.brokencircuits.kissad.streamepisodelink.domain.EpisodePageDetails;
import com.brokencircuits.kissad.streamepisodelink.streamprocessing.ShowProcessor;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import java.util.List;
import org.apache.kafka.streams.processor.ProcessorSupplier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(TopicAutoconfig.class)
public class KafkaConfig {

  @Bean
  ProcessorSupplier<ByteKey<ShowMsgKey>, ShowMsg> showProcessorSupplier(KissWebFetcher webFetcher,
      Extractor<HtmlPage, List<EpisodePageDetails>> episodePageDetailExtractor,
      Publisher<ByteKey<KissEpisodePageKey>, KissEpisodePageMessage> kissEpisodePageMessagePublisher) {
    return () -> new ShowProcessor(webFetcher, episodePageDetailExtractor,
        kissEpisodePageMessagePublisher);
  }

//  @Bean
//  CommandLineRunner testRunner(
//      ProcessorSupplier<ByteKey<ShowMsgKey>, ShowMsg> showProcessorSupplier) {
//    return args -> {
//      Processor<ByteKey<ShowMsgKey>, ShowMsg> processor = showProcessorSupplier.get();
//
//      ShowMsgKey key = ShowMsgKey.newBuilder()
//          .setShowId(Uuid.randomUUID())
//          .build();
//
//      Map<String, String> sources = new HashMap<>();
//      sources.put(SourceName.KISSANIME.name(),
//          "https://kissanime.ru/Anime/Full-Time-Magister-4th-Season");
//
//      ShowMsg value = ShowMsg.newBuilder()
//          .setKey(key)
//          .setValue(ShowMsgValue.newBuilder()
//              .setTitle("Full-Time Magister")
//              .setSeason(4)
//              .setSources(sources)
//              .setIsActive(true)
//              .setReleaseScheduleCron("0 0 15 * * *")
//              .setSkipEpisodeString(null)
//              .setEpisodeNamePattern("S<SEASON_2>E<EPISODE_2>")
//              .setFolderName("FullTime Magister")
//              .setMessageId(Uuid.randomUUID())
//              .build())
//          .build();
//
//      processor.process(new ByteKey<>(key), value);
//    };
//  }

}

