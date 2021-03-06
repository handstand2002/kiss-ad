package com.brokencircuits.kissad.restshow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class RestShowApplication {

  public static void main(String[] args) {
    SpringApplication.run(RestShowApplication.class, args);
  }

}
