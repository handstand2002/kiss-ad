package com.brokencircuits.kissad.spshowfetch.config;

import com.brokencircuits.kissad.kafka.ByteKey;
import com.brokencircuits.kissad.messages.ShowMsg;
import com.brokencircuits.kissad.messages.ShowMsgKey;
import com.brokencircuits.kissad.messages.SourceName;
import com.brokencircuits.kissad.spshowfetch.dslprocessing.ShowProcessor;
import com.brokencircuits.kissad.spshowfetch.dslprocessing.ShowProcessor.ShowProcessorArgs;
import java.util.Arrays;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.processor.ProcessorSupplier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class ProcessorConfig {

  @Bean
  SourceName acceptSource(@Value("${fetcher.type}") String sourceType) {
    try {
      return SourceName.valueOf(sourceType);
    } catch (Exception e) {
      log.error("fetcher.type must be one of [{}]",
          Arrays.stream(SourceName.values())
              .map(Object::toString)
              .collect(Collectors.joining(", ")));
      throw new RuntimeException(e);
    }
  }

  @Bean
  ProcessorSupplier<ByteKey<ShowMsgKey>, ShowMsg> showProcessorSupplier(
      ShowProcessorArgs args) {
    return () -> new ShowProcessor(args);
  }
}
