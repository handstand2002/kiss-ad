package com.brokencircuits.kissad.restshow.rest;

import com.brokencircuits.kissad.kafka.KeyValueStore;
import com.brokencircuits.kissad.kafka.Publisher;
import com.brokencircuits.kissad.messages.KissShowMessage;
import com.brokencircuits.kissad.restshow.rest.domain.AddShowRequest;
import java.util.concurrent.atomic.AtomicLong;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ShowRestController {

  private final Publisher<Long, KissShowMessage> showMessagePublisher;
  private final KeyValueStore<Long, KissShowMessage> showMessageStore;

  private final AtomicLong highestAssignedId = new AtomicLong(0);

  private static final String CONTENT_TYPE_JSON = "application/json";

  @PostMapping(path = "/addShow", consumes = CONTENT_TYPE_JSON, produces = CONTENT_TYPE_JSON)
  public AddShowRequest addShow(@RequestBody AddShowRequest showRequest) {
    KissShowMessage message = KissShowMessage.newBuilder()
        .setUrl(showRequest.getShowUrl())
        .setName(showRequest.getShowName())
        .setSeasonNumber(showRequest.getSeasonNumber())
        .setIsActive(true).build();

    // if we haven't gone through the store to find the highest ID yet, do it now
    if (highestAssignedId.get() == 0) {
      findHighestAssignedId();
    }

    Long showId = showRequest.getShowId();
    if (showId == null) {
      showId = highestAssignedId.incrementAndGet();
    }
    showMessagePublisher.send(showId, message);

    return showRequest;
  }

  private void findHighestAssignedId() {
    KeyValueIterator<Long, KissShowMessage> iter = showMessageStore.getStore().all();

    while (iter.hasNext()) {
      KeyValue<Long, KissShowMessage> entry = iter.next();
      if (entry.key > highestAssignedId.get()) {
        highestAssignedId.set(entry.key);
      }
    }
    iter.close();
  }
}
