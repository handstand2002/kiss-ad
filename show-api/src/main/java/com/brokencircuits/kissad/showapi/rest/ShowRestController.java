package com.brokencircuits.kissad.showapi.rest;

import com.brokencircuits.kissad.Translator;
import com.brokencircuits.kissad.kafka.AdminInterface;
import com.brokencircuits.kissad.kafka.ByteKey;
import com.brokencircuits.kissad.kafka.KeyValueStoreWrapper;
import com.brokencircuits.kissad.kafka.Publisher;
import com.brokencircuits.kissad.messages.ShowMsg;
import com.brokencircuits.kissad.messages.ShowMsgKey;
import com.brokencircuits.kissad.showapi.rest.domain.ShowObject;
import com.brokencircuits.kissad.topics.TopicUtil;
import com.brokencircuits.kissad.util.Uuid;
import com.brokencircuits.messages.Command;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.KeyValue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class ShowRestController {

  private final Publisher<ByteKey<ShowMsgKey>, ShowMsg> showMessagePublisher;
  private final Translator<ShowObject, KeyValue<ByteKey<ShowMsgKey>, ShowMsg>> showLocalToMsgTranslator;
  private final Translator<KeyValue<ByteKey<ShowMsgKey>, ShowMsg>, ShowObject> showMsgToLocalTranslator;
  private final KeyValueStoreWrapper<ByteKey<ShowMsgKey>, ShowMsg> showMsgStore;
  private final AdminInterface adminInterface;

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
      newShowObject.setShowId(Uuid.randomUUID());
    }

    if (newShowObject.getEpisodeNamePattern() == null) {
      newShowObject.setEpisodeNamePattern(defaultEpisodeNamePattern);
    }

    if (newShowObject.getReleaseScheduleCron() == null) {
      newShowObject.setReleaseScheduleCron(defaultReleaseScheduleCron);
    }

    KeyValue<ByteKey<ShowMsgKey>, ShowMsg> msg = showLocalToMsgTranslator.translate(newShowObject);

    if (msg.value.getValue().getSkipEpisodeString() != null) {
      adminInterface.sendCommand(TopicUtil.MODULE_DOWNLOAD_DELEGATOR, Command.SKIP_EPISODE_RANGE,
          msg.value.getKey().getShowId().toString(), msg.value.getValue().getSkipEpisodeString());

      msg.value.getValue().setSkipEpisodeString(null);
    }

    showMessagePublisher.send(msg);
    return newShowObject;
  }

  @GetMapping(path = "/checkShow", produces = CONTENT_TYPE_JSON)
  public List<ShowObject> checkShow() {
    List<ShowObject> outputList = new ArrayList<>();
    showMsgStore.all()
        .forEachRemaining(pair -> outputList.add(checkShow(pair.value.getKey().getShowId())));

    return outputList;
  }

  @GetMapping(path = "/checkShow/{id}", produces = CONTENT_TYPE_JSON)
  public ShowObject checkShow(@PathVariable final Uuid id) {
    ByteKey<ShowMsgKey> lookupKey = ByteKey.from(ShowMsgKey.newBuilder().setShowId(id).build());
    ShowMsg showMessage = showMsgStore.get(lookupKey);

    if (showMessage != null) {
      adminInterface
          .sendCommand(TopicUtil.MODULE_SCHEDULER, Command.CHECK_NEW_EPISODES, id.toString());
    }

    return showMessage != null ? showMsgToLocalTranslator
        .translate(KeyValue.pair(lookupKey, showMessage)) : null;
  }


  @GetMapping(path = "/getShow", produces = CONTENT_TYPE_JSON)
  public List<ShowObject> getShowList() {
    List<ShowObject> outputList = new ArrayList<>();
    showMsgStore.all()
        .forEachRemaining(pair -> outputList.add(showMsgToLocalTranslator.translate(pair)));
    return outputList;
  }

  @GetMapping(path = "/getShow/{id}", produces = CONTENT_TYPE_JSON)
  public ShowObject getShow(@PathVariable final Uuid id) {
    ByteKey<ShowMsgKey> lookupKey = ByteKey.from(ShowMsgKey.newBuilder().setShowId(id).build());
    ShowMsg showMessage = showMsgStore.get(lookupKey);

    return showMessage != null ? showMsgToLocalTranslator
        .translate(KeyValue.pair(lookupKey, showMessage)) : null;
  }

  @DeleteMapping(path = "/deleteShow/{id}", produces = CONTENT_TYPE_JSON)
  public ShowObject deleteShow(@PathVariable final Uuid id) {
    ShowMsgKey key = ShowMsgKey.newBuilder().setShowId(id).build();
    ByteKey<ShowMsgKey> lookupKey = ByteKey.from(key);
    ShowMsg showMessage = showMsgStore.get(lookupKey);
    ShowObject showObject = null;
    if (showMessage != null) {
      showObject = showMsgToLocalTranslator
          .translate(new KeyValue<>(lookupKey, showMessage));
    }
    if (showObject != null) {
      showMessagePublisher.send(lookupKey, ShowMsg.newBuilder().setKey(key).setValue(null).build());
    }
    return showObject;
  }
}
