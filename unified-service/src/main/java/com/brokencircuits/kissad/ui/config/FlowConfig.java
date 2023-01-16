package com.brokencircuits.kissad.ui.config;

import com.brokencircuits.download.messages.DownloadType;
import com.brokencircuits.downloader.messages.DownloadRequestMsg;
import com.brokencircuits.downloader.messages.DownloadRequestValue;
import com.brokencircuits.kissad.download.domain.SimpleDownloadResult;
import com.brokencircuits.kissad.messages.EpisodeMsg;
import com.brokencircuits.kissad.messages.EpisodeMsgKey;
import com.brokencircuits.kissad.messages.ShowMsg;
import com.brokencircuits.kissad.messages.ShowMsgKey;
import com.brokencircuits.kissad.table.ReadWriteTable;
import com.brokencircuits.kissad.ui.delegator.DelegatorController;
import com.brokencircuits.kissad.ui.downloader.aria.AriaResponseStatus;
import com.brokencircuits.kissad.ui.downloader.controller.DownloadController;
import com.brokencircuits.kissad.ui.fetcher.FetcherController;
import com.brokencircuits.kissad.ui.scheduler.SchedulerController;
import com.brokencircuits.kissad.util.ByteKey;
import com.brokencircuits.kissad.util.Uuid;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
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
  BiConsumer<ByteKey<ShowMsgKey>, ShowMsg> onShowUpdate(SchedulerController controller) {
    return (key, msg) -> {
      log.info("Show was updated, updating schedule for {}", key);
      controller.scheduleShow(key, msg);
      log.info("Finished updating schedule for show {}", key);
    };
  }

  @Bean
  Consumer<Uuid> triggerShowCheckMethod(FetcherController fetcherController) {
    return showUuid -> {
      try {
        log.info("Checking for new episodes for show {}", showUuid);
        fetcherController.fetch(showUuid);
        log.info("Finished checking for new episodes for {}", showUuid);
      } catch (Exception e) {
        log.error("Could not checking for new episodes for {} due to ", showUuid, e);
      }
    };
  }

  @Bean
  Consumer<EpisodeMsg> notifyOfEpisode(DelegatorController controller) {
    return msg -> {
      try {
        log.info("Found episode ShowId {}, Episode {}", msg.getKey().getShowId(),
            msg.getKey().getEpisodeNumber());
        controller.process(msg);
        log.info("Finished processing ShowId {}, Episode {}", msg.getKey().getShowId(),
            msg.getKey().getEpisodeNumber());
      } catch (Exception e) {
        log.error("Could not complete processing of {} due to ", msg, e);
      }
    };
  }

  @Bean
  Function<DownloadRequestMsg, CompletableFuture<SimpleDownloadResult>> onDownloadRequest(
      DownloadController controller,
      ReadWriteTable<ByteKey<EpisodeMsgKey>, EpisodeMsg> episodeTable) {
    return request -> {
      try {
        DownloadRequestValue req = request.getValue();

        boolean isMagnet = request.getKey().getDownloadType().equals(DownloadType.MAGNET);

        CompletableFuture<SimpleDownloadResult> future = new CompletableFuture<>();
        Consumer<AriaResponseStatus> onStatusPoll = status -> {
          String pcntComplete = String.format("%.2f",
              (100 * ((double) status.getResult().getCompletedLength() / status.getResult()
                  .getTotalLength())));
          log.info("{} {}% complete", status.getResult().getFiles().get(0).getPath(), pcntComplete);
        };

        try {
          controller.doDownload(req.getUri(), req.getDestinationDir(), req.getDestinationFileName(),
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
