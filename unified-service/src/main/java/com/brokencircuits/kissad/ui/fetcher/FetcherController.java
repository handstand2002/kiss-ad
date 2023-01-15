package com.brokencircuits.kissad.ui.fetcher;

import com.brokencircuits.kissad.kafka.ByteKey;
import com.brokencircuits.kissad.kafka.table.KafkaBackedTable;
import com.brokencircuits.kissad.messages.ShowMsg;
import com.brokencircuits.kissad.messages.ShowMsgKey;
import com.brokencircuits.kissad.util.Uuid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class FetcherController {

  private final SpFetcher spFetcher;
  private final KafkaBackedTable<ByteKey<ShowMsgKey>, ShowMsg> showTable;

  public void fetch(Uuid showUuid) {

    ShowMsgKey showKey = ShowMsgKey.newBuilder()
        .setShowId(showUuid)
        .build();
    ByteKey<ShowMsgKey> key = new ByteKey<>(showKey);
    ShowMsg showMsg = showTable.get(key);
    if (showMsg == null) {
      log.error("Show doesn't exist for Uuid: {}", showUuid);
      return;
    }
    if (showMsg.getValue().getSources().containsKey(SpFetcher.SOURCE_IDENTIFIER)) {
      spFetcher.process(key, showMsg);
    } else {
      log.error("Received message for fetchers: {}", showMsg.getValue().getSources().keySet());
    }
  }
}
