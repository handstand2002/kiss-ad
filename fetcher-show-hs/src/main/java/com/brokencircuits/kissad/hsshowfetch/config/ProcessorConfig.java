package com.brokencircuits.kissad.hsshowfetch.config;

import com.brokencircuits.kissad.Extractor;
import com.brokencircuits.kissad.hsshowfetch.dslprocessing.ShowProcessor;
import com.brokencircuits.kissad.hsshowfetch.fetch.HsEpisodeFetcher;
import com.brokencircuits.kissad.kafka.ByteKey;
import com.brokencircuits.kissad.kafka.Publisher;
import com.brokencircuits.kissad.messages.EpisodeMsg;
import com.brokencircuits.kissad.messages.EpisodeMsgKey;
import com.brokencircuits.kissad.messages.ShowMsg;
import com.brokencircuits.kissad.messages.ShowMsgKey;
import com.gargoylesoftware.htmlunit.WebClient;
import org.apache.kafka.streams.processor.ProcessorSupplier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ProcessorConfig {

  @Bean
  ProcessorSupplier<ByteKey<ShowMsgKey>, ShowMsg> showProcessorSupplier(
      WebClient webclient,
      Extractor<String, Long> showIdExtractor,
      HsEpisodeFetcher hsEpisodeFetcher,
      Publisher<ByteKey<EpisodeMsgKey>, EpisodeMsg> publisher) {
    return () -> new ShowProcessor(webclient, showIdExtractor, hsEpisodeFetcher, publisher);
  }
}
