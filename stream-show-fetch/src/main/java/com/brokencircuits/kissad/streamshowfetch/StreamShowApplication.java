package com.brokencircuits.kissad.streamshowfetch;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@Slf4j
@EnableScheduling
@SpringBootApplication
public class StreamShowApplication {

  public static void main(String[] args) {
    SpringApplication.run(StreamShowApplication.class, args);
  }
}
