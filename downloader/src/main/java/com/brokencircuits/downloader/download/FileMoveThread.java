package com.brokencircuits.downloader.download;

import com.brokencircuits.downloader.domain.AriaResponseStatus;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FileMoveThread extends Thread {

  private final static Pattern FILE_EXTENSION_PATTERN = Pattern.compile("\\.(\\w+)$");

  public FileMoveThread(String destinationDir, String outputFilename,
                        BiConsumer<File, AriaResponseStatus> onDownloadComplete, String downloadedToFilename,
                        AriaResponseStatus latestStatus, boolean overwritePermission, int renameRetryCount,
                        Duration renameRetryDelay) {
    super(() -> {
      File downloaded = new File(downloadedToFilename);
      log.info("Downloaded: {}", downloaded);

      String resolvedOutputFilename = resolveOutputExtension(downloaded.getAbsolutePath(),
          outputFilename);

      File file = new File(destinationDir);
      if (!file.exists()) {
        if (!file.mkdirs()) {
          log.warn("Unable to create folder structure for destination file: {}",
              file.getAbsolutePath());
        }
      }
      String newFilePath = destinationDir + resolvedOutputFilename;

      File destinationFile = new File(newFilePath);
      if (destinationFile.exists() && overwritePermission) {
        if (!destinationFile.delete()) {
          log.warn("Unable to overwrite existing file {}", destinationFile.getAbsolutePath());
        }
      }

      boolean copied = false;
      boolean deleted = false;
      int renameAttempt = 0;
      do {
        log.info("Trying to rename file from {} to {}", downloaded.getAbsolutePath(), newFilePath);
        try {
          Path newPath = new File(newFilePath).toPath();
          maybeDelete(newPath);
          Files.copy(downloaded.toPath(), newPath);
          copied = true;
          Files.delete(downloaded.toPath());
          deleted = true;
        } catch (IOException e) {
          log.info("Failed file finalization (Copied={}, DeleteOriginal={}). attempt {} of {} - {} " +
                  "to {}; waiting {}ms", copied, deleted, ++renameAttempt, renameRetryCount,
              downloaded.getAbsolutePath(), newFilePath, renameRetryDelay.toMillis(), e);
          trySleep(renameRetryDelay.toMillis());
        }
      } while (!deleted && renameAttempt < renameRetryCount);

      log.info("Successful in renaming file: {}", copied);
      onDownloadComplete.accept(downloaded, latestStatus);
    });
  }

  private static void maybeDelete(Path newPath) {
    try {
      Files.delete(newPath);
    } catch (NoSuchFileException e) {
      // expected
    } catch (IOException e) {
      log.error("Could not delete existing file {}", newPath, e);
    }
  }

  private static void trySleep(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  private static String resolveOutputExtension(String downloadedFullPath,
                                               String requestedFilename) {
    String downloadedExt = fileExtension(downloadedFullPath);
    String desiredExt = fileExtension(requestedFilename);

    if (!downloadedExt.equals(desiredExt)) {
      requestedFilename += "." + downloadedExt;
      log.info("Appending original extension ({}) to desired filename. New: {}", downloadedExt,
          requestedFilename);
    }
    return requestedFilename;
  }

  private static String fileExtension(String path) {
    Matcher matcher = FILE_EXTENSION_PATTERN.matcher(path);
    if (matcher.find()) {
      return matcher.group(1);
    }
    return "";
  }

}
