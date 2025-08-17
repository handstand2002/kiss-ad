package com.brokencircuits.kissad.controller;

import com.brokencircuits.kissad.domain.RequestEpisodeOperation;
import com.brokencircuits.kissad.domain.ShowDto;
import com.brokencircuits.kissad.domain.api.*;
import com.brokencircuits.kissad.repository.ShowRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.scheduling.support.SimpleTriggerContext;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.text.SimpleDateFormat;
import java.util.*;

@Slf4j
@Controller
@RequiredArgsConstructor
public class WsMsgController {
  private static final SimpleDateFormat NEXT_EPISODE_DATE_FORMAT = new SimpleDateFormat("EEE h:mma");
  private static final String SHOWS_TOPIC = "/topic/shows";
  private static final String USER_QUEUE = "/queue/reply";

  private final SimpMessagingTemplate messagingTemplate;
  private final ShowRepository showRepository;
  private final RequestEpisodeOperation requestEpisodeOperation;

  @MessageMapping("/requests")
  public void handleRequest(ServerMsg message, Principal principal) {

    log.info("Received msg for principal {}: {}", principal.getName(), message);
    HandlerCtx ctx = new HandlerCtx(messagingTemplate, principal.getName());
    if (message instanceof InitMsg) {
      handleInitRequest((InitMsg) message, ctx);
      return;
    } else if (message instanceof ShowUpdateMsg) {
      updateShow((ShowUpdateMsg) message, ctx);
      return;
    }
  }

  private void updateShow(ShowUpdateMsg message, HandlerCtx ctx) {
    ShowDto dto = ShowDto.builder()
        .id(message.getDetails().getId())
        .title(message.getDetails().getTitle())
        .season(message.getDetails().getSeason())
        .releaseScheduleCron(message.getDetails().getReleaseScheduleCron())
        .skipEpisodeString(message.getDetails().getSkipEpisodeString())
        .episodeNamePattern(message.getDetails().getEpisodeNamePattern())
        .folderName(message.getDetails().getFolderName())
        .sourceName(message.getDetails().getSourceName())
        .url(message.getDetails().getUrl())
        .isActive(message.getDetails().getIsActive())
        .nextEpisode(message.getDetails().getNextEpisode())
        .build();
    showRepository.save(dto);
    ShowListingMsg msg = createListingMsg(dto);

    ctx.broadcast(SHOWS_TOPIC, msg);
    ctx.sendToUser(msg);
  }

  private void handleInitRequest(InitMsg message, HandlerCtx ctx) {
    if (message.getPage() == UiPage.SHOWS) {

      List<ShowDto> allShows = showRepository.findAll();
      for (ShowDto dto : allShows) {
        ShowListingMsg msg = createListingMsg(dto);
        ctx.sendToUser(msg);
      }
    } else if (message.getPage() == UiPage.SHOW) {
      Object showId = message.getParams().get("id");
      if (showId == null) {
        // TODO: send error to UI
        log.error("Could not find showId for message {}", message);
        return;
      }
      Optional<ShowDto> dto = showRepository.findById(String.valueOf(showId));
      if (!dto.isPresent()) {
        // TODO: send error to UI
        return;
      }
      ShowListingMsg msg = createListingMsg(dto.get());
      ctx.sendToUser(msg);
    } else {
      log.error("Unsupported message {}", message);
    }
  }

  @RequiredArgsConstructor
  private static class HandlerCtx {
    private final SimpMessagingTemplate messagingTemplate;
    private final String principalName;

    public void sendToUser(Object msg) {
      log.info("Sending msg to user {}: {}", principalName, msg);
      messagingTemplate.convertAndSendToUser(principalName, USER_QUEUE, msg);
    }

    public void broadcast(String topic, Object msg) {
      log.info("Broadcasting msg: {}", msg);
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

    String nextEpisodeString = getNextEpisodeString(showDto.getReleaseScheduleCron());
    builder.nextEpisode(nextEpisodeString);

    return builder.build();
  }

  @NotNull
  private static String getNextEpisodeString(String releaseScheduleCron) {
    String nextEpisodeString;
    try {
      Date nextRun = nextRunTime(releaseScheduleCron);
      nextEpisodeString = NEXT_EPISODE_DATE_FORMAT.format(nextRun);
    } catch (Exception e) {
      nextEpisodeString = "ERR";
    }
    return nextEpisodeString;
  }

  private static Date nextRunTime(String cron) {
    return nextRunTimeFromTime(cron, null);
  }

  private static Date nextRunTimeFromTime(String cron, Date fromTime) {
    if (fromTime == null) {
      fromTime = new Date();
    }
    CronTrigger trigger1 = new CronTrigger(cron);

    return trigger1.nextExecutionTime(new SimpleTriggerContext(fromTime, fromTime, fromTime));
  }

}
