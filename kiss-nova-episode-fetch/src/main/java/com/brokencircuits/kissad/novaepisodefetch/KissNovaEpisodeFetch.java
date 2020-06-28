package com.brokencircuits.kissad.novaepisodefetch;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class KissNovaEpisodeFetch {

  public static void main(String[] args) {
    SpringApplication.run(KissNovaEpisodeFetch.class, args);
  }

}
