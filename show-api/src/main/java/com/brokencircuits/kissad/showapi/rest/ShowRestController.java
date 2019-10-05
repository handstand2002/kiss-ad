package com.brokencircuits.kissad.showapi.rest;

import com.brokencircuits.kissad.Translator;
import com.brokencircuits.kissad.kafka.KeyValueStore;
import com.brokencircuits.kissad.kafka.Publisher;
import com.brokencircuits.kissad.messages.ShowMessage;
import com.brokencircuits.kissad.showapi.rest.domain.ShowObject;
import java.util.ArrayList;
import java.util.List;
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

  private final Publisher<Long, ShowMessage> showMessagePublisher;
  private final Translator<ShowObject, KeyValue<Long, ShowMessage>> showLocalToMsgTranslator;
  private final Translator<KeyValue<Long, ShowMessage>, ShowObject> showMsgToLocalTranslator;
  private final KeyValueStore<Long, ShowMessage> showMsgStore;

  private Long highestAssignedId = null;

  private static final String CONTENT_TYPE_JSON = "application/json";

  /**
   * <pre>
   *   {
   * 	   Optional "showId",
   * 	   Optional "isActive",
   *     Optional "initialSkipEpisodeString",
   *     Required "title": ,
   *     Nullable "season",
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

    // set show ID if it isn't present (if show ID isn't present, assume it's a new show entry)
    if (newShowObject.getShowId() == null) {
      newShowObject.setShowId(getNextShowId());
    }

    KeyValue<Long, ShowMessage> message = showLocalToMsgTranslator.translate(newShowObject);

    showMessagePublisher.send(message);
    return newShowObject;
  }

  private Long getNextShowId() {
    if (highestAssignedId == null) {
      highestAssignedId = 0L;
      showMsgStore.all().forEachRemaining(pair -> {
        if (pair.key > highestAssignedId) {
          highestAssignedId = pair.key;
        }
      });
    }
    return ++highestAssignedId;
  }

  @GetMapping(path = "/getShow", produces = CONTENT_TYPE_JSON)
  public List<ShowObject> getShowList() {
    List<ShowObject> outputList = new ArrayList<>();
    showMsgStore.all()
        .forEachRemaining(pair -> outputList.add(showMsgToLocalTranslator.translate(pair)));
    return outputList;
  }

  @GetMapping(path = "/getShow/{id}", produces = CONTENT_TYPE_JSON)
  public ShowObject getShow(@PathVariable final Long id) {
    ShowMessage showMessage = showMsgStore.get(id);

    return showMessage != null ? showMsgToLocalTranslator
        .translate(new KeyValue<>(id, showMessage)) : null;
  }

  @DeleteMapping(path = "/deleteShow/{id}", produces = CONTENT_TYPE_JSON)
  public ShowObject deleteShow(@PathVariable final Long id) {
    ShowMessage showMessage = showMsgStore.get(id);
    ShowObject showObject = null;
    if (showMessage != null) {
      showObject = showMsgToLocalTranslator
          .translate(new KeyValue<>(id, showMessage));
    }
    if (showObject != null) {
      showMessagePublisher.send(id, null);
    }
    return showObject;
  }
}
