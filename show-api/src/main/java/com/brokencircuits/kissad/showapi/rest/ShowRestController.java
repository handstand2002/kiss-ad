package com.brokencircuits.kissad.showapi.rest;

import com.brokencircuits.kissad.Translator;
import com.brokencircuits.kissad.kafka.KeyValueStoreWrapper;
import com.brokencircuits.kissad.kafka.Publisher;
import com.brokencircuits.kissad.messages.ShowMsgKey;
import com.brokencircuits.kissad.messages.ShowMsgValue;
import com.brokencircuits.kissad.showapi.rest.domain.ShowObject;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.streams.KeyValue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ShowRestController {

  private final Publisher<ShowMsgKey, ShowMsgValue> showMessagePublisher;
  private final Translator<ShowObject, KeyValue<ShowMsgKey, ShowMsgValue>> showLocalToMsgTranslator;
  private final Translator<KeyValue<ShowMsgKey, ShowMsgValue>, ShowObject> showMsgToLocalTranslator;
  private final KeyValueStoreWrapper<ShowMsgKey, ShowMsgValue> showMsgStore;

  @Value("${show.default.episode-name-pattern}")
  private String defaultEpisodeNamePattern;
  @Value("${show.default.release-schedule-cron}")
  private String defaultReleaseScheduleCron;

  private static final String CONTENT_TYPE_JSON = "application/json";

  /**
   * <pre>
   *   {
   * 	   Optional "showId",     // if absent, ID will be assigned
   * 	   Optional "isActive",   // default to True
   *     Optional "initialSkipEpisodeString",
   *     Required "title",
   *     Nullable "season",
   *     Optional "episodeNamePattern", // if not set, uses default set in config
   *     Required "folderName",
   *     Required "sources": [
   *         {
   *             "sourceName": "KISSANIME",
   *             "url": "https://kissanime.ru/Anime/One-Punch-Man-Season-2"
   *         },
   *         ...
   *     ]
   * }
   * </pre>
   */
  @PostMapping(path = "/addShow", consumes = CONTENT_TYPE_JSON, produces = CONTENT_TYPE_JSON)
  public ShowObject addShow(@RequestBody ShowObject newShowObject) {

    if (newShowObject.getIsActive() == null) {
      newShowObject.setIsActive(true);
    }

    if (newShowObject.getShowId() == null) {
      newShowObject.setShowId(UUID.randomUUID());
    }

    if (newShowObject.getEpisodeNamePattern() == null) {
      newShowObject.setEpisodeNamePattern(defaultEpisodeNamePattern);
    }

    if (newShowObject.getReleaseScheduleCron() == null) {
      newShowObject.setReleaseScheduleCron(defaultReleaseScheduleCron);
    }

    KeyValue<ShowMsgKey, ShowMsgValue> message = showLocalToMsgTranslator.translate(newShowObject);

    showMessagePublisher.send(message);
    return newShowObject;
  }

  @GetMapping(path = "/getShow", produces = CONTENT_TYPE_JSON)
  public List<ShowObject> getShowList() {
    List<ShowObject> outputList = new ArrayList<>();
    showMsgStore.all()
        .forEachRemaining(pair -> outputList.add(showMsgToLocalTranslator.translate(pair)));
    return outputList;
  }

  @GetMapping(path = "/getShow/{id}", produces = CONTENT_TYPE_JSON)
  public ShowObject getShow(@PathVariable final String id) {
    ShowMsgKey lookupKey = ShowMsgKey.newBuilder().setShowId(id).build();
    ShowMsgValue showMessage = showMsgStore.get(lookupKey);

    return showMessage != null ? showMsgToLocalTranslator
        .translate(new KeyValue<>(lookupKey, showMessage)) : null;
  }

  @DeleteMapping(path = "/deleteShow/{id}", produces = CONTENT_TYPE_JSON)
  public ShowObject deleteShow(@PathVariable final String id) {
    ShowMsgKey lookupKey = ShowMsgKey.newBuilder().setShowId(id).build();
    ShowMsgValue showMessage = showMsgStore.get(lookupKey);
    ShowObject showObject = null;
    if (showMessage != null) {
      showObject = showMsgToLocalTranslator
          .translate(new KeyValue<>(lookupKey, showMessage));
    }
    if (showObject != null) {
      showMessagePublisher.send(lookupKey, null);
    }
    return showObject;
  }
}
