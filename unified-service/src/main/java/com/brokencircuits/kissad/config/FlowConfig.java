package com.brokencircuits.kissad.config;

import com.brokencircuits.kissad.controller.DelegatorController;
import com.brokencircuits.kissad.controller.DownloadController;
import com.brokencircuits.kissad.controller.FetcherController;
import com.brokencircuits.kissad.controller.SchedulerController;
import com.brokencircuits.kissad.domain.CheckShowOperation;
import com.brokencircuits.kissad.domain.CheckShowResult;
import com.brokencircuits.kissad.domain.RequestEpisodeOperation;
import com.brokencircuits.kissad.domain.RequestEpisodeResult;
import com.brokencircuits.kissad.domain.ShowDto;
import com.brokencircuits.kissad.download.domain.DownloadRequest;
import com.brokencircuits.kissad.download.domain.DownloadType;
import com.brokencircuits.kissad.download.domain.SimpleDownloadResult;
import com.brokencircuits.kissad.downloader.aria.AriaResponseStatus;
import com.brokencircuits.kissad.repository.EpisodeRepository;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;

@Slf4j
@Configuration
public class FlowConfig {

  @Bean
  TaskExecutor taskExecutor() {
    return new ConcurrentTaskExecutor();
  }

  @Bean
  TaskScheduler taskScheduler() {
    return new ConcurrentTaskScheduler();
  }

  @Bean
  Consumer<ShowDto> onShowUpdate(SchedulerController controller) {
    return msg -> {
      log.info("Show was updated, updating schedule for {}", msg);
      controller.scheduleShow(msg);
      log.info("Finished updating schedule for show {}", msg);
    };
  }

  @Bean
  CheckShowOperation triggerShowCheckMethod(FetcherController fetcherController) {
    return showUuid -> {
      try {
        log.info("Checking for new episodes for show {}", showUuid);
        CheckShowResult result = fetcherController.fetch(showUuid);
        log.info("Finished checking for new episodes for {}", showUuid);
        return result;
      } catch (Exception e) {
        log.error("Could not checking for new episodes for {} due to ", showUuid, e);
        return new CheckShowResult(0, true);
      }
    };
  }

  @Bean
  RequestEpisodeOperation notifyOfEpisode(DelegatorController controller) {
    return msg -> {
      try {
        log.info("Found episode ShowId {}, Episode {}", msg.getShowId(),
            msg.getEpisodeNumber());
        RequestEpisodeResult result = controller.process(msg);
        log.info("Finished processing ShowId {}, Episode {}", msg.getShowId(),
            msg.getEpisodeNumber());
        return result;
      } catch (Exception e) {
        log.error("Could not complete processing of {} due to ", msg, e);
        return new RequestEpisodeResult(false, true);
      }
    };
  }

  @Bean
  Function<DownloadRequest, CompletableFuture<SimpleDownloadResult>> onDownloadRequest(
      DownloadController controller,
      EpisodeRepository episodeRepository) {
    return request -> {
      try {

        boolean isMagnet = request.getType().equals(DownloadType.MAGNET);

        CompletableFuture<SimpleDownloadResult> future = new CompletableFuture<>();
        Consumer<AriaResponseStatus> onStatusPoll = status -> {
          String pcntComplete = String.format("%.2f",
              (100 * ((double) status.getResult().getCompletedLength() / status.getResult()
                  .getTotalLength())));
          log.info("{} {}% complete", status.getResult().getFiles().get(0).getPath(), pcntComplete);
        };

        try {
          controller.doDownload(request.getUri(), request.getDestinationDir(),
              request.getDestinationFileName(),
              isMagnet, onStatusPoll, (f, s) -> {
                if (s.getResult().getErrorCode() == 0) {
                  log.info("Download completed");
                } else {
                  log.info("Download failed");
                }
                future.complete(new SimpleDownloadResult(s.getResult().getErrorCode()));
              });
        } catch (IOException | InterruptedException e) {
          log.error("Could not complete download: {}", request, e);
          future.complete(null);
        }
        return future;
      } catch (Exception e) {
        log.error("Could not complete download of {} due to ", request, e);
      }
      return CompletableFuture.completedFuture(null);
    };
  }
}
