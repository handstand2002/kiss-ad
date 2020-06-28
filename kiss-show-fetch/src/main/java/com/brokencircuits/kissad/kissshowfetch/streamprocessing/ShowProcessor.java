package com.brokencircuits.kissad.kissshowfetch.streamprocessing;

import com.brokencircuits.kissad.Extractor;
import com.brokencircuits.kissad.kafka.ByteKey;
import com.brokencircuits.kissad.kafka.Publisher;
import com.brokencircuits.kissad.kissshowfetch.domain.EpisodePageDetails;
import com.brokencircuits.kissad.kissweb.KissWebFetcher;
import com.brokencircuits.kissad.messages.KissEpisodePageKey;
import com.brokencircuits.kissad.messages.KissEpisodePageMessage;
import com.brokencircuits.kissad.messages.ShowMsg;
import com.brokencircuits.kissad.messages.ShowMsgKey;
import com.brokencircuits.kissad.messages.SourceName;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.processor.Processor;
import org.apache.kafka.streams.processor.ProcessorContext;


@Slf4j
@RequiredArgsConstructor
public class ShowProcessor implements Processor<ByteKey<ShowMsgKey>, ShowMsg> {

  private final KissWebFetcher webClient;
  private final Extractor<HtmlPage, List<EpisodePageDetails>> episodePageDetailExtractor;
  private final Publisher<ByteKey<KissEpisodePageKey>, KissEpisodePageMessage> kissEpisodePageMessagePublisher;

  @Override
  public void init(ProcessorContext context) {
  }

  @Override
  public void process(ByteKey<ShowMsgKey> key, ShowMsg msg) {
    log.info("Processing show: {} | {}", key, msg);
    String showUrl = msg.getValue().getSources().get(SourceName.KISSANIME.toString());
    try {
      HtmlPage page = webClient.fetchPage(showUrl);

      List<EpisodePageDetails> episodePageDetails = episodePageDetailExtractor.extract(page);

      episodePageDetails.stream()
          .map(episode -> {
            KissEpisodePageKey episodePageKey = KissEpisodePageKey.newBuilder()
                .setEpisodeName(episode.getEpisodeName())
                .setShowKey(msg.getKey())
                .build();
            KissEpisodePageMessage episodePageMsg = KissEpisodePageMessage.newBuilder()
                .setKey(episodePageKey)
                .setUrl(episode.getEpisodeUrl())
                .setEpisodeName(episode.getEpisodeName())
                .setEpisodeNumber(episode.getEpisodeNumber())
                .setRetrieveTime(Instant.now())
                .build();
            return KeyValue.pair(new ByteKey<>(episodePageKey), episodePageMsg);
          })
          .forEach(kissEpisodePageMessagePublisher::send);

    } catch (Exception e) {
      log.error("Unable to process {} due to Exception", msg, e);
    }
  }

  @Override
  public void close() {

  }
}
