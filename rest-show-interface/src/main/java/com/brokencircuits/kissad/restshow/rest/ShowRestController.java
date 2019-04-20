package com.brokencircuits.kissad.restshow.rest;

import com.brokencircuits.kissad.kafka.KeyValueStore;
import com.brokencircuits.kissad.kafka.Publisher;
import com.brokencircuits.kissad.messages.ShowMessage;
import com.brokencircuits.kissad.restshow.rest.domain.AddShowRequest;
import com.brokencircuits.kissad.restshow.rest.domain.AddShowResponse;
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

  private final Publisher<Long, ShowMessage> showMessagePublisher;
  private final KeyValueStore<Long, ShowMessage> showMessageStore;

  private final AtomicLong highestAssignedId = new AtomicLong(0);

  private static final String CONTENT_TYPE_JSON = "application/json";

  @PostMapping(path = "/addShow", consumes = CONTENT_TYPE_JSON, produces = CONTENT_TYPE_JSON)
  public AddShowResponse addShow(@RequestBody AddShowRequest showRequest) {
    ShowMessage message = ShowMessage.newBuilder()
        .setUrl(showRequest.getShowUrl())
        .setName(showRequest.getShowName())
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

    return new AddShowResponse(showRequest.getShowUrl(), showRequest.getShowName(), showId);
  }

  private void findHighestAssignedId() {
    KeyValueIterator<Long, ShowMessage> iter = showMessageStore.getStore().all();

    while (iter.hasNext()) {
      KeyValue<Long, ShowMessage> entry = iter.next();
      if (entry.key > highestAssignedId.get()) {
        highestAssignedId.set(entry.key);
      }
    }
  }
}
