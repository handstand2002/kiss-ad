package com.brokencircuits.kissad.restshow.stores;

import com.brokencircuits.kissad.kafka.KeyValueStore;
import com.brokencircuits.kissad.messages.KissShowMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ShowMessageStore extends KeyValueStore<Long, KissShowMessage> {

  public ShowMessageStore(@Value("${messaging.stores.show}") String storeName) {
    super(storeName);
  }
}
