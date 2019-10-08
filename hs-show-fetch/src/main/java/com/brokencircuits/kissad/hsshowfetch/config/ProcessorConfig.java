package com.brokencircuits.kissad.hsshowfetch.config;

import com.brokencircuits.kissad.Extractor;
import com.brokencircuits.kissad.hsshowfetch.dslprocessing.ShowProcessor;
import com.brokencircuits.kissad.hsshowfetch.fetch.HsEpisodeFetcher;
import com.brokencircuits.kissad.kafka.Publisher;
import com.brokencircuits.kissad.messages.EpisodeMsgKey;
import com.brokencircuits.kissad.messages.EpisodeMsgValue;
import com.brokencircuits.kissad.messages.ShowMsgKey;
import com.brokencircuits.kissad.messages.ShowMsgValue;
import com.gargoylesoftware.htmlunit.WebClient;
import org.apache.kafka.streams.processor.ProcessorSupplier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ProcessorConfig {

  @Bean
  ProcessorSupplier<ShowMsgKey, ShowMsgValue> showProcessorSupplier(
      WebClient webclient,
      Extractor<String, Long> showIdExtractor,
      HsEpisodeFetcher hsEpisodeFetcher,
      Publisher<EpisodeMsgKey, EpisodeMsgValue> publisher) {
    return () -> new ShowProcessor(webclient, showIdExtractor, hsEpisodeFetcher, publisher);
  }
}
