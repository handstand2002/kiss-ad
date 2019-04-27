package com.brokencircuits.kissad.restshow.rest;

import com.brokencircuits.kissad.Translator;
import com.brokencircuits.kissad.kafka.KeyValueStore;
import com.brokencircuits.kissad.kafka.Publisher;
import com.brokencircuits.kissad.messages.KissShowMessage;
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
  private final Translator<ShowObject, KissShowMessage> showLocalToMessageTranslator;
  private final Translator<KeyValue<Long, KissShowMessage>, ShowObject> showMessageToLocalTranslator;
  private final KeyValueStore<Long, KissShowMessage> showMessageStore;

  private final AtomicLong highestAssignedId = new AtomicLong(0);

  private static final String CONTENT_TYPE_JSON = "application/json";

  @PostMapping(path = "/addShow", consumes = CONTENT_TYPE_JSON, produces = CONTENT_TYPE_JSON)
  public ShowObject addShow(@RequestBody ShowObject newShowObject) {
    KissShowMessage message = showLocalToMessageTranslator.translate(newShowObject);

    // if we haven't gone through the store to find the highest ID yet, do it now
    if (highestAssignedId.get() == 0) {
      findHighestAssignedId();
    }

    Long showId = newShowObject.getShowId();
    if (showId == null) {
      showId = highestAssignedId.incrementAndGet();
    }
    showMessagePublisher.send(showId, message);

    return newShowObject;
  }

  @GetMapping(path = "/getShow/all", consumes = CONTENT_TYPE_JSON, produces = CONTENT_TYPE_JSON)
  public List<ShowObject> getShowList() {
    List<ShowObject> outputList = new ArrayList<>();
    showMessageStore.asMap().forEach((showId, showMsg) -> outputList
        .add(showMessageToLocalTranslator.translate(new KeyValue<>(showId, showMsg))));
    return outputList;
  }

  @GetMapping(path = "/getShow/{id}")
  public ShowObject getShow(@PathVariable final Long id) {
    KissShowMessage showMessage = showMessageStore.asMap().get(id);

    return showMessage != null ? showMessageToLocalTranslator
        .translate(new KeyValue<>(id, showMessage)) : null;
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

  private void findHighestAssignedId() {
    showMessageStore.asMap().forEach((showId, kissShowMessage) -> {
      if (highestAssignedId.get() < showId) {
        highestAssignedId.set(showId);
      }
    });
  }
}
