package com.brokencircuits.kissad.controller;


import com.brokencircuits.kissad.fetcher.SpFetcher;
import com.brokencircuits.kissad.messages.ShowMsg;
import com.brokencircuits.kissad.messages.ShowMsgKey;
import com.brokencircuits.kissad.table.ReadWriteTable;
import com.brokencircuits.kissad.util.Uuid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class FetcherController {

  private final SpFetcher spFetcher;
  private final ReadWriteTable<ShowMsgKey, ShowMsg> showTable;

  public void fetch(Uuid showUuid) {

    ShowMsgKey showKey = ShowMsgKey.newBuilder()
        .setShowId(showUuid)
        .build();
    ShowMsg showMsg = showTable.get(showKey);
    if (showMsg == null) {
      log.error("Show doesn't exist for Uuid: {}", showUuid);
      return;
    }
    if (showMsg.getValue().getSources().containsKey(SpFetcher.SOURCE_IDENTIFIER)) {
      spFetcher.process(showKey, showMsg);
    } else {
      log.error("Received message for fetchers: {}", showMsg.getValue().getSources().keySet());
    }
  }
}
