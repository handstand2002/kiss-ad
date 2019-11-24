package com.brokencircuits.kissad.delegator.config;

import com.brokencircuits.kissad.delegator.dslprocessing.EpisodeProcessor;
import com.brokencircuits.kissad.download.DownloadApi;
import com.brokencircuits.kissad.kafka.ByteKey;
import com.brokencircuits.kissad.kafka.Publisher;
import com.brokencircuits.kissad.kafka.StateStoreDetails;
import com.brokencircuits.kissad.messages.EpisodeMsg;
import com.brokencircuits.kissad.messages.EpisodeMsgKey;
import com.brokencircuits.kissad.messages.ShowMsg;
import com.brokencircuits.kissad.messages.ShowMsgKey;
import org.apache.kafka.streams.processor.ProcessorSupplier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ProcessorConfig {

  @Bean
  ProcessorSupplier<ByteKey<EpisodeMsgKey>, EpisodeMsg> processorSupplier(DownloadApi downloadApi,
      StateStoreDetails<ByteKey<ShowMsgKey>, ShowMsg> showStoreDetails,
      Publisher<ByteKey<EpisodeMsgKey>, EpisodeMsg> episodeStorePublisher,
      StateStoreDetails<ByteKey<EpisodeMsgKey>, EpisodeMsg> episodeStoreDetails) {
    // TODO: make options configurable
    return () -> new EpisodeProcessor(downloadApi, episodeStorePublisher, showStoreDetails,
        episodeStoreDetails, 99999, "Anime");
  }
}
