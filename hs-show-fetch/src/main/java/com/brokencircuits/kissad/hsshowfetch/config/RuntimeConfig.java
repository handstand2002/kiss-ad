package com.brokencircuits.kissad.hsshowfetch.config;

import com.brokencircuits.kissad.kafka.StreamsService;
import com.brokencircuits.kissad.kafka.StreamsServiceRunner;
import java.util.Collection;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RuntimeConfig {

  @Bean
  CommandLineRunner startStreams(StreamsServiceRunner streamsRunner) {
    return args -> streamsRunner.run();
  }

  @Bean
  StreamsServiceRunner serviceRunner(Collection<StreamsService> services) {
    return new StreamsServiceRunner(services);
  }

}
