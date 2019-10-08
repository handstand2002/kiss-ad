package com.brokencircuits.kissad.delegator.config;

import com.brokencircuits.kissad.delegator.dslprocessing.EpisodeProcessor;
import com.brokencircuits.kissad.download.DownloadApi;
import com.brokencircuits.kissad.kafka.StateStoreDetails;
import com.brokencircuits.kissad.messages.EpisodeMsgKey;
import com.brokencircuits.kissad.messages.EpisodeMsgValue;
import com.brokencircuits.kissad.messages.ShowMsgKey;
import com.brokencircuits.kissad.messages.ShowMsgValue;
import org.apache.kafka.streams.processor.ProcessorSupplier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ProcessorConfig {

  @Bean
  ProcessorSupplier<EpisodeMsgKey, EpisodeMsgValue> processorSupplier(DownloadApi downloadApi,
      StateStoreDetails<ShowMsgKey, ShowMsgValue> showStoreDetails) {
    // TODO: make options configurable
    return () -> new EpisodeProcessor(downloadApi, showStoreDetails, 99999, "Anime");
  }
}
