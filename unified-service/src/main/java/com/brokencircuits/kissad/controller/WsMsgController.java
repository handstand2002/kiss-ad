package com.brokencircuits.kissad.controller;

import com.brokencircuits.kissad.domain.CheckShowOperation;
import com.brokencircuits.kissad.domain.EpisodeId;
import com.brokencircuits.kissad.domain.ShowDto;
import com.brokencircuits.kissad.domain.api.*;
import com.brokencircuits.kissad.domain.downloader.DownloadStatus;
import com.brokencircuits.kissad.domain.internal.DownloadStatusUpdatedEvent;
import com.brokencircuits.kissad.repository.EpisodeRepository;
import com.brokencircuits.kissad.repository.ShowRepository;
import com.brokencircuits.kissad.service.DownloaderService;
import com.brokencircuits.kissad.service.ShowDownloaderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.event.EventListener;
import org.springframework.core.task.TaskExecutor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.scheduling.support.SimpleTriggerContext;
import org.springframework.stereotype.Controller;

import java.io.IOException;
import java.security.Principal;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Controller
@RequiredArgsConstructor
public class WsMsgController {
  public static final DateTimeFormatter NEXT_RELEASE_FORMATTER = DateTimeFormatter.ofPattern("EEE h:mma");
  private static final DateTimeFormatter DOWNLOAD_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd h:mm a");

  private final SimpMessagingTemplate messagingTemplate;
  private final ShowRepository showRepository;
  private final EpisodeRepository episodeRepository;
  private final CheckShowOperation triggerShowCheckMethod;
  private final TaskExecutor taskExecutor;
  private final DownloaderService downloaderService;
//  private final ShowDownloaderService showDownloaderService;

  @MessageMapping("/shows/init")
  public void handleInit(GenericInitMsg request, Principal principal) {
    log.info("Received msg from {}: {}", principal.getName(), request);
    HandlerCtx ctx = new HandlerCtx(messagingTemplate, principal.getName());
    List<ShowDto> allShows = showRepository.findAll();
    for (ShowDto dto : allShows) {
      ShowListingMsg msg = createListingMsg(dto);
      ctx.sendToUser(msg);
    }
  }

  @MessageMapping("/show/init")
  public void handleShowInit(ShowPageInitMsg request, Principal principal) {
    HandlerCtx ctx = new HandlerCtx(messagingTemplate, principal.getName());

    Object showId = request.getShowId();
    if (showId == null) {
      // TODO: send error to UI
      log.error("Could not find showId for message {}", request);
      return;
    }
    Optional<ShowDto> dto = showRepository.findById(String.valueOf(showId));
    if (!dto.isPresent()) {
      // TODO: send error to UI
      return;
    }

    Optional<ShowDto> showDto = showRepository.findById(request.getShowId());
    if (!showDto.isPresent()) {
      // TODO: send error
      return;
    }

    ShowListingMsg showMsg = createListingMsg(showDto.get());
    ctx.sendToUser(showMsg);
  }

  @MessageMapping("/show-episodes/init")
  public void handleInit(ShowPageInitMsg request, Principal principal) {
    HandlerCtx ctx = new HandlerCtx(messagingTemplate, principal.getName());

    Object showId = request.getShowId();
    if (showId == null) {
      // TODO: send error to UI
      log.error("Could not find showId for message {}", request);
      return;
    }
    Optional<ShowDto> dto = showRepository.findById(String.valueOf(showId));
    if (!dto.isPresent()) {
      // TODO: send error to UI
      return;
    }

    Optional<ShowDto> showDto = showRepository.findById(request.getShowId());
    if (!showDto.isPresent()) {
      // TODO: send error
      return;
    }

    ShowListingMsg showMsg = createListingMsg(showDto.get());
    ctx.sendToUser(showMsg);

    Map<Long, ShowEpisodeListingMsg> downloadedEpisodes = new HashMap<>();

    episodeRepository.findByShowId(request.getShowId())
        .forEach(epDto -> downloadedEpisodes.put((long) epDto.getEpisodeNumber(),
            ShowEpisodeListingMsg.builder()
                .downloadedQuality(epDto.getDownloadedQuality())
                .downloadTime(epDto.getDownloadTime()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime()
                    .format(DOWNLOAD_TIME_FORMATTER))
                .episodeNumber(epDto.getEpisodeNumber())
                .build()));

    for (ShowEpisodeListingMsg epDto : downloadedEpisodes.values()) {
      ctx.sendToUser(epDto);
    }
  }

  @MessageMapping("/show/update")
  public void handleShowUpdate(ShowListingMsg request, Principal principal) {
    log.info("Received msg from {}: {}", principal.getName(), request);
    HandlerCtx ctx = new HandlerCtx(messagingTemplate, principal.getName());
    updateShow(request, ctx);
  }

  @MessageMapping("/show/episode/delete")
  public void handleEpDelete(EpDeleteMsg request, Principal principal) {
    log.info("Received msg from {}: {}", principal.getName(), request);
    HandlerCtx ctx = new HandlerCtx(messagingTemplate, principal.getName());
    deleteEpisode(request, ctx);
  }

  @MessageMapping("/show/check-new")
  public void handleCheckNewRequest(CheckShowRequestMsg request, Principal principal) {
    log.info("Received msg from {}: {}", principal.getName(), request);
    HandlerCtx ctx = new HandlerCtx(messagingTemplate, principal.getName());
//    showDownloaderService.checkNewEpisodes(request.getShowId());
    handleCheckNewEpisodes(request.getShowId(), ctx);
  }

  @MessageMapping("/downloads/new")
  public void handleNewDownloadRequest(NewDownloadRequestMsg request, Principal principal) {
    log.info("Received msg from {}: {}", principal.getName(), request);
    HandlerCtx ctx = new HandlerCtx(messagingTemplate, principal.getName());

    try {
      downloaderService.submitDownload(request.getUrl(), request.getDestination());
    } catch (IOException | InterruptedException e) {
      log.error("Exception submitting download to aria: {}", request, e);
    }
  }

  @EventListener
  public void handleDownloadServiceStatusUpdate(DownloadStatus status) {
    log.info("Publishing to UI: {}", status);
    messagingTemplate.convertAndSend(WebSocketTopics.TOPIC_DL_SVC_STATUS, status);
  }

  @EventListener
  public void handleDownloadStatusUpdate(DownloadStatusUpdatedEvent event) {
    messagingTemplate.convertAndSend(WebSocketTopics.TOPIC_DL_STATUS, event);
  }

  private void handleCheckNewEpisodes(String showId, HandlerCtx ctx) {
    Optional<ShowDto> show = showRepository.findById(showId);

    if (show.isPresent()) {
      taskExecutor.execute(() -> triggerShowCheckMethod.run(showId));
    }
  }

  private void deleteEpisode(EpDeleteMsg request, HandlerCtx ctx) {
    episodeRepository.deleteById(EpisodeId.builder()
        .showId(request.getShowId())
        .episodeNumber(request.getEpisodeNumber())
        .build());

    String sendUpdateToTopic = WebSocketTopics.episodeUpdates(request.getShowId());
    ctx.broadcast(sendUpdateToTopic, ShowEpisodeListingMsg.builder()
        .isDelete(true)
        .episodeNumber(request.getEpisodeNumber())
        .build());
  }

  private void updateShow(ShowListingMsg message, HandlerCtx ctx) {
    if (StringUtils.isBlank(message.getId())) {
      // new show creation
      message.setId(UUID.randomUUID().toString());
    }
    ShowDto dto = ShowDto.builder()
        .id(message.getId())
        .title(message.getTitle())
        .season(message.getSeason())
        .releaseScheduleCron(message.getReleaseScheduleCron())
        .skipEpisodeString(message.getSkipEpisodeString())
        .episodeNamePattern(message.getEpisodeNamePattern())
        .folderName(message.getFolderName())
        .sourceName(message.getSourceName())
        .url(message.getUrl())
        .isActive(message.getIsActive())
        .nextEpisode(message.getNextEpisode())
        .build();
    showRepository.save(dto);
    ShowListingMsg msg = createListingMsg(dto);

    ctx.broadcast(WebSocketTopics.SHOWS_TOPIC, msg);
    ctx.sendToUser(msg);
  }

  @RequiredArgsConstructor
  private static class HandlerCtx {
    private final SimpMessagingTemplate messagingTemplate;
    private final String principalName;

    public void sendToUser(Object msg) {
      log.info("Sending msg to user {}: {}", principalName, msg);
      messagingTemplate.convertAndSendToUser(principalName, WebSocketTopics.USER_QUEUE, msg);
    }

    public void broadcast(String topic, Object msg) {
      log.info("Broadcasting msg to topic {}: {}", topic, msg);
      messagingTemplate.convertAndSend(topic, msg);
    }
  }

  private static ShowListingMsg createListingMsg(ShowDto showDto) {

    ShowListingMsg.ShowListingMsgBuilder builder = ShowListingMsg.builder()
        .id(showDto.getId())
        .title(showDto.getTitle())
        .season(showDto.getSeason())
        .releaseScheduleCron(showDto.getReleaseScheduleCron())
        .skipEpisodeString(showDto.getSkipEpisodeString())
        .episodeNamePattern(showDto.getEpisodeNamePattern())
        .folderName(showDto.getFolderName())
        .sourceName(showDto.getSourceName())
        .url(showDto.getUrl())
        .isActive(showDto.getIsActive());

    String nextEpisodeString;
    long secondsToNextCheck;
    try {
      Instant nextRun = nextRunTime(showDto.getReleaseScheduleCron());
      nextEpisodeString = nextRun.atZone(ZoneId.systemDefault()).format(NEXT_RELEASE_FORMATTER);
      secondsToNextCheck = nextRun.getEpochSecond() - Instant.now().getEpochSecond();
    } catch (Exception e) {
      nextEpisodeString = "ERR";
      secondsToNextCheck = Duration.ofDays(15).getSeconds();
    }

    builder.nextEpisode(nextEpisodeString);
    builder.secondsToNextCheck(secondsToNextCheck);

    return builder.build();
  }

  private static Instant nextRunTime(String cron) {
    return nextRunTimeFromTime(cron, null);
  }

  private static Instant nextRunTimeFromTime(String cron, Date fromTime) {
    if (fromTime == null) {
      fromTime = new Date();
    }
    CronTrigger trigger1 = new CronTrigger(cron);

    Date date = trigger1.nextExecutionTime(new SimpleTriggerContext(fromTime, fromTime, fromTime));
    return date.toInstant();
  }

}
