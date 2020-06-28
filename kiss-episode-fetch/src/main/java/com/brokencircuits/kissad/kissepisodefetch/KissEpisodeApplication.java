package com.brokencircuits.kissad.kissepisodefetch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class KissEpisodeApplication {

  public static void main(String[] args) {
    SpringApplication.run(KissEpisodeApplication.class, args);
  }

}
