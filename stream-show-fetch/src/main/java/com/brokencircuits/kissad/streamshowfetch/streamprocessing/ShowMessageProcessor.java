package com.brokencircuits.kissad.streamshowfetch.streamprocessing;

import com.brokencircuits.kissad.kafka.Publisher;
import com.brokencircuits.kissad.kissweb.KissWebFetcher;
import com.brokencircuits.kissad.messages.KissEpisodePageKey;
import com.brokencircuits.kissad.messages.KissEpisodePageMessage;
import com.brokencircuits.kissad.messages.KissShowMessage;
import com.brokencircuits.kissad.streamshowfetch.extractor.EpisodeMessageExtractor;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.processor.Processor;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ShowMessageProcessor implements Processor<Long, KissShowMessage> {


  final private KissWebFetcher webFetcher;
  final private Publisher<KissEpisodePageKey, KissEpisodePageMessage> episodeMessagePublisher;
  final private EpisodeMessageExtractor extractor;

  @Override
  public void init(ProcessorContext processorContext) {

  }

  @Override
  public void process(Long showId, KissShowMessage showMessage) {
    // on failure to fully process, throw into a "failed" topic

    log.info("Processing {} | {}", showId, showMessage);

    String url = showMessage.getUrl();
    try {
      HtmlPage htmlPage = webFetcher.fetchPage(url);

      List<KeyValue<KissEpisodePageKey, KissEpisodePageMessage>> episodeObjectList = extractor
          .extract(new KeyValue<>(showMessage, htmlPage));

      // TODO: use globalKTable of the topic this produces to in order to deduplicate messages

      // send all messages in order
      for (KeyValue<KissEpisodePageKey, KissEpisodePageMessage> kissEpisodePageKeyKissEpisodePageMessageKeyValue : episodeObjectList) {
        Future<RecordMetadata> recordFuture = episodeMessagePublisher
            .send(kissEpisodePageKeyKissEpisodePageMessageKeyValue);
        try {
          recordFuture.get();   // wait for it to be inserted
        } catch (InterruptedException | ExecutionException e) {
          e.printStackTrace();
        }
      }

    } catch (IOException | URISyntaxException | IllegalArgumentException e) {
      log.error("Error Processing show page: {}", e.getMessage());
    }
  }

  @Override
  public void close() {

  }
}
