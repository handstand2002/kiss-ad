package com.brokencircuits.kissad.showapi.rest;

import com.brokencircuits.kissad.kafka.Publisher;
import com.brokencircuits.kissad.showapi.rest.domain.PublishString;
import com.brokencircuits.kissad.showapi.streams.StreamController;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class StreamRestController {

  private final Publisher<String, String> stringPublisher;
  private final StreamController streamsService;

  private static final String CONTENT_TYPE_JSON = "application/json";

  @PostMapping(path = "/publish", consumes = CONTENT_TYPE_JSON, produces = CONTENT_TYPE_JSON)
  public PublishString addShow(@RequestBody PublishString stringObj) {

    stringPublisher
        .send(stringObj.getKey(),
            stringObj.getValue() + " " + Instant.now().toString() + " " + stringObj.getTopic(),
            stringObj.getTopic());
    return stringObj;
  }

  @GetMapping(path = "/start")
  public String startStreams() {
    streamsService.start();
    return "Started";
  }

  @GetMapping(path = "/stop")
  public String stopStreams() {
    streamsService.stop();
    return "Stopped";
  }

}
