package com.brokencircuits.kissad.streamepdownload.download;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

@Slf4j
public class DownloadUtil {

  public static boolean tryDownloadFile(String url, File destination, int attempts)
      throws MalformedURLException {
    URL toDownload = new URL(url);

    log.info("Downloading {} to {}", toDownload, destination);
    int currentAttempt = 0;
    while (currentAttempt < attempts) {
      currentAttempt++;
      log.info("Attempt {}/{}", currentAttempt, attempts);
      try {
        FileUtils.copyURLToFile(toDownload, destination);
        return true;
      } catch (IOException expected) {
        log.warn("Download failed: {}", expected.getMessage());
      }
    }
    return false;
  }
}
