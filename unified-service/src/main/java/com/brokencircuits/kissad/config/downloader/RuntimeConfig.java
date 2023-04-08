package com.brokencircuits.kissad.config.downloader;

import com.brokencircuits.kissad.domain.downloader.config.AriaProps;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Duration;
import java.time.Instant;

@Slf4j
@Configuration
public class RuntimeConfig {

  @Bean
  ProcessBuilder ariaProcess(AriaProps ariaProps) {
    return new ProcessBuilder(ariaProps.getAriaPath(), "--enable-rpc=true",
        "--rpc-listen-port=" + ariaProps.getAriaRpcPort(), "--allow-overwrite=true", "--pause");
  }

  @Bean
  CommandLineRunner startAria(ProcessBuilder ariaProcess) {
    return args -> {
      log.info("Starting Aria");
      Instant startTime = Instant.now();
      Instant changeLoggingTime = startTime.plus(Duration.ofSeconds(30));
      Process runningAria = ariaProcess.start();
      new Thread(() -> {
        InputStream is = runningAria.getInputStream();
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);
        String line;

        try {
          while ((line = br.readLine()) != null) {
            if (!line.trim().isEmpty()) {
              if (Instant.now().isBefore(changeLoggingTime)) {
                log.info("Aria: {}", line);
              } else {
                log.debug("Aria: {}", line);
              }
            }
          }
        } catch (IOException e) {
          log.error("Exception while trying to read from Aria:", e);
        }
      }).start();

      Runtime.getRuntime().addShutdownHook(new Thread(runningAria::destroy));
    };
  }
}
