package com.brokencircuits.kissad.streamepisodelink;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class KissShowApplication {

  public static void main(String[] args) {
    SpringApplication.run(KissShowApplication.class, args);
  }
}
