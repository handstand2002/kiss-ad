package com.brokencircuits.downloader.download;

import com.brokencircuits.downloader.domain.AriaResponseStatus;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.brokencircuits.downloader.domain.download.DownloadResult;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FileMoveThread extends Thread {

  private final static Pattern FILE_EXTENSION_PATTERN = Pattern.compile("\\.(\\w+)$");

  public FileMoveThread(File destinationDir, String outputFilename,
                        BiConsumer<File, AriaResponseStatus> onDownloadComplete, String downloadedToFilename,
                        AriaResponseStatus latestStatus, boolean overwritePermission, int renameRetryCount,
                        Duration renameRetryDelay) {
    super(() -> {
      File downloaded = new File(downloadedToFilename);
      log.info("Downloaded: {}; exists: {}", downloaded, downloaded.exists());

      String resolvedOutputFilename = resolveOutputExtension(downloaded.getAbsolutePath(),
          outputFilename);

      if (!destinationDir.exists()) {
        if (!destinationDir.mkdirs()) {
          log.warn("Unable to create folder structure for destination file: {}",
              destinationDir.getAbsolutePath());
        }
      }

      File destinationFile = parseFile(destinationDir.getAbsolutePath(), resolvedOutputFilename);

      if (destinationFile.exists() && overwritePermission) {
        if (!destinationFile.delete()) {
          log.warn("Unable to overwrite existing file {}", destinationFile.getAbsolutePath());
        }
      }

      boolean copied = false;
      boolean deleted = false;
      int renameAttempt = 0;
      Path newPath = destinationFile.toPath();
      Path source = downloaded.toPath();
      do {
        log.info("Trying to rename file from {} to {}", source, newPath);
        try {
          if (!copied) {
            maybeDelete(newPath);
            Files.copy(source, newPath);
            copied = true;
          }
          Files.delete(source);
          deleted = true;
        } catch (IOException e) {
          log.info("Failed file finalization (Copied={}, DeleteOriginal={}). attempt {} of {} - {} " +
                  "to {}; waiting {}ms", copied, deleted, ++renameAttempt, renameRetryCount,
              source, newPath, renameRetryDelay.toMillis(), e);
          trySleep(renameRetryDelay.toMillis());
        }
      } while (!deleted && renameAttempt < renameRetryCount);

      log.info("Successful in renaming file: {}", copied);
      if (!copied) {
        onDownloadComplete.accept(downloaded, new AriaResponseStatus(latestStatus.getId(), latestStatus.getRpcVersion(), latestStatus.getResult().toBuilder()
            .errorCode(404)
            .errorMessage("Could not copy file to final destination")
            .build()));
      } else {
        onDownloadComplete.accept(downloaded, latestStatus);
      }
    });
  }

  private static File parseFile(String filePath, String... children) {
    File file = null;
    String[] split = filePath.split("[/\\\\]");

    for (String s : split) {
      if (file == null) {
        file = new File(s);
      } else {
        file = new File(file, s);
      }
    }

    Objects.requireNonNull(file);
    for (String child : children) {
      String[] splitChild = child.split("[/\\\\]");

      for (String s : splitChild) {
        file = new File(file, s);
      }
    }

    return file;
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
