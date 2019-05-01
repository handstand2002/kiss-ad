package com.brokencircuits.kissad.restshow.rest;

import com.brokencircuits.kissad.Translator;
import com.brokencircuits.kissad.kafka.KeyValueStore;
import com.brokencircuits.kissad.kafka.Publisher;
import com.brokencircuits.kissad.messages.DownloadedEpisodeKey;
import com.brokencircuits.kissad.messages.DownloadedEpisodeMessage;
import com.brokencircuits.kissad.messages.KissShowMessage;
import com.brokencircuits.kissad.restshow.poll.PollNewEpisodeScheduleController;
import com.brokencircuits.kissad.restshow.rest.domain.EpisodeKey;
import com.brokencircuits.kissad.restshow.rest.domain.ShowObject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.streams.KeyValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ShowRestController {

  private final Publisher<Long, KissShowMessage> showMessagePublisher;
  private final Publisher<DownloadedEpisodeKey, DownloadedEpisodeMessage> completedEpisodePublisher;
  private final Translator<ShowObject, KissShowMessage> showLocalToMessageTranslator;
  private final Translator<KeyValue<Long, KissShowMessage>, ShowObject> showMessageToLocalTranslator;
  private final KeyValueStore<Long, KissShowMessage> showMessageStore;
  private final KeyValueStore<String, Long> showIdLookupStore;
  private final PollNewEpisodeScheduleController pollNewEpisodeScheduleController;

  private final AtomicLong highestAssignedId = new AtomicLong(0);

  private static final String CONTENT_TYPE_JSON = "application/json";

  @PostMapping(path = "/addShow", consumes = CONTENT_TYPE_JSON, produces = CONTENT_TYPE_JSON)
  public ShowObject addShow(@RequestBody ShowObject newShowObject) {
    if (newShowObject.getIsActive() == null) {
      newShowObject.setIsActive(true);
    }
    KissShowMessage message = showLocalToMessageTranslator.translate(newShowObject);

    Long showId;
    if (newShowObject.getShowId() != null) {
      showId = newShowObject.getShowId();
    } else {
      Long existingShowId = showIdLookupStore.asMap().get(message.getUrl());
      if (existingShowId != null) {
        showId = existingShowId;
      } else {
        // if we haven't gone through the store to find the highest ID yet, do it now
        if (highestAssignedId.get() == 0) {
          findHighestAssignedId();
        }
        showId = highestAssignedId.incrementAndGet();
      }
    }

    showMessagePublisher.send(showId, message);
    return newShowObject.toBuilder().showId(showId).build();
  }

  @GetMapping(path = "/getShow", produces = CONTENT_TYPE_JSON)
  public List<ShowObject> getShowList() {
    List<ShowObject> outputList = new ArrayList<>();
    showMessageStore.asMap().forEach((showId, showMsg) -> outputList
        .add(showMessageToLocalTranslator.translate(new KeyValue<>(showId, showMsg))));
    return outputList;
  }

  @GetMapping(path = "/getShow/{id}", produces = CONTENT_TYPE_JSON)
  public ShowObject getShow(@PathVariable final Long id) {
    KissShowMessage showMessage = showMessageStore.asMap().get(id);

    return showMessage != null ? showMessageToLocalTranslator
        .translate(new KeyValue<>(id, showMessage)) : null;
  }

  @GetMapping(path = "/poll")
  public String submitPoll() {
    if (pollNewEpisodeScheduleController.trySendPoll(false)) {
      return "Successfully send poll request";
    } else {
      return "Scheduled poll";
    }
  }

  @DeleteMapping(path = "/deleteShow/{id}", produces = CONTENT_TYPE_JSON)
  public ShowObject deleteShow(@PathVariable final Long id) {
    KissShowMessage showMessage = showMessageStore.asMap().get(id);
    ShowObject showObject = null;
    if (showMessage != null) {
      showObject = showMessageToLocalTranslator
          .translate(new KeyValue<>(id, showMessage));
    }
    if (showObject != null) {
      showMessagePublisher.send(id, null);
    }
    return showObject;
  }

  @PostMapping(path = "/requestEpisode", consumes = CONTENT_TYPE_JSON, produces = CONTENT_TYPE_JSON)
  public EpisodeKey requestReDownload(@RequestBody EpisodeKey episodeKey) {

    DownloadedEpisodeKey key = DownloadedEpisodeKey.newBuilder()
        .setSubOrDub(episodeKey.getSubOrDub())
        .setSeasonNumber(episodeKey.getSeasonNumber())
        .setEpisodeNumber(episodeKey.getEpisodeNumber())
        .setEpisodeName(episodeKey.getEpisodeName())
        .setShowName(episodeKey.getShowName())
        .build();
    completedEpisodePublisher.send(key, null);
    submitPoll();

    return episodeKey;
  }

  private void findHighestAssignedId() {
    showMessageStore.asMap().forEach((showId, kissShowMessage) -> {
      if (highestAssignedId.get() < showId) {
        highestAssignedId.set(showId);
      }
    });
  }
}
