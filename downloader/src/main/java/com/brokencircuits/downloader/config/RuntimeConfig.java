package com.brokencircuits.downloader.config;

import com.brokencircuits.downloader.configprops.AriaProps;
import com.brokencircuits.downloader.download.DownloadController;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class RuntimeConfig {

  @Bean
  CommandLineRunner startAria(ProcessBuilder ariaProcess) {
    return args -> {
      Process runningAria = ariaProcess.start();
      new Thread(() -> {
        InputStream is = runningAria.getInputStream();
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);
        String line;

        try {
          while ((line = br.readLine()) != null) {
            if (!line.trim().isEmpty()) {
              log.info("Aria: {}", line);
            }
          }
        } catch (IOException e) {
          log.error("Exception while trying to read from Aria:", e);
        }
      }).start();

      Runtime.getRuntime().addShutdownHook(new Thread(runningAria::destroy));
    };
  }

  @Bean
  CommandLineRunner sendCommandToAria(DownloadController controller) {
    return args -> {
      Thread.sleep(2000);
      String uri = "magnet:?xt=urn:btih:A2PKXUUWXL5VSJCEOK2GICGCXFSXNLD4&tr=http://nyaa.tracker.wf:7777/announce&tr=udp://tracker.coppersurfer.tk:6969/announce&tr=udp://tracker.internetwarriors.net:1337/announce&tr=udp://tracker.leechersparadise.org:6969/announce&tr=udp://tracker.opentrackr.org:1337/announce&tr=udp://open.stealth.si:80/announce&tr=udp://p4p.arenabg.com:1337/announce&tr=udp://mgtracker.org:6969/announce&tr=udp://tracker.tiny-vps.com:6969/announce&tr=udp://peerfect.org:6969/announce&tr=http://share.camoe.cn:8080/announce&tr=http://t.nyaatracker.com:80/announce&tr=https://open.kickasstracker.com:443/announce";
//      String uri = "http://ipv4.download.thinkbroadband.com/50MB.zip";
      controller.doDownload(uri, true);
    };
  }

  @Bean
  ProcessBuilder ariaProcess(AriaProps ariaProps) {
    return new ProcessBuilder(ariaProps.getAriaPath(), "--enable-rpc=true",
        "--rpc-listen-port=" + ariaProps.getAriaRpcPort(), "--allow-overwrite=true", "--pause");
  }


}
