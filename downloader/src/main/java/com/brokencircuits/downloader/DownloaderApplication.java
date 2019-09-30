package com.brokencircuits.downloader;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Slf4j
@SpringBootApplication
public class DownloaderApplication {

  public static void main(String[] args) {
    SpringApplication.run(DownloaderApplication.class, args);
  }
}
