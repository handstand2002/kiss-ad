package com.brokencircuits.kissad.spshowfetch.dslprocessing;

import com.brokencircuits.download.messages.DownloadType;
import com.brokencircuits.kissad.Extractor;
import com.brokencircuits.kissad.kafka.ByteKey;
import com.brokencircuits.kissad.kafka.Publisher;
import com.brokencircuits.kissad.messages.EpisodeLink;
import com.brokencircuits.kissad.messages.EpisodeMsg;
import com.brokencircuits.kissad.messages.EpisodeMsgKey;
import com.brokencircuits.kissad.messages.EpisodeMsgValue;
import com.brokencircuits.kissad.messages.ShowMsg;
import com.brokencircuits.kissad.messages.ShowMsgKey;
import com.brokencircuits.kissad.messages.SourceName;
import com.brokencircuits.kissad.spshowfetch.domain.ShowEpisodeResponse;
import com.brokencircuits.kissad.spshowfetch.domain.SpEpisode;
import com.brokencircuits.kissad.spshowfetch.domain.SpEpisodeDownloadEntry;
import com.brokencircuits.kissad.spshowfetch.fetch.SpShowEpisodeClient;
import com.brokencircuits.kissad.util.Uuid;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.processor.Processor;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
public class ShowProcessor implements Processor<ByteKey<ShowMsgKey>, ShowMsg> {

  private final ShowProcessorArgs args;

  @Value
  @RequiredArgsConstructor
  @Component
  public static class ShowProcessorArgs {
    SourceName acceptSource;
    WebClient webClient;
    Extractor<String, Long> showIdExtractor;
    Extractor<String, Long> showEpisodeExtractor;
    SpShowEpisodeClient feignSpShowEpisodeFetcher;
    Publisher<ByteKey<EpisodeMsgKey>, EpisodeMsg> episodeMsgPublisher;
  }

  @Override
  public void init(ProcessorContext context) {
  }

  @Override
  public void process(ByteKey<ShowMsgKey> key, ShowMsg msg) {
    log.info("Processing show: {} | {}", key, msg);
    String url = msg.getValue().getSources().get(args.getAcceptSource().toString());
    try {
      String htmlPage = fetchPageFromUrl(url);
      Long showId = args.getShowIdExtractor().extract(htmlPage);
      ShowEpisodeResponse episodeResponse = args.getFeignSpShowEpisodeFetcher()
          .findAll(String.valueOf(showId));

      episodeResponse.getEpisode().entrySet().stream()
          .map(e -> {
            try {
              Long epNumber = args.getShowEpisodeExtractor().extract(e.getKey());
              return convertEpisodeObj(e.getValue(), epNumber, msg.getKey());
            } catch (Exception exception) {
              log.error("Unable to process episode {} | {} due to error", e.getKey(), e.getValue(), exception);
              return null;
            }
          })
          .filter(Objects::nonNull)
          .sorted(Comparator.comparingLong(kv -> kv.value.getKey().getEpisodeNumber()))
          .forEach(kv -> args.getEpisodeMsgPublisher().send(kv));

    } catch (Exception e) {
      log.error("Unable to process {} due to Exception", msg, e);
    }
  }

  private KeyValue<ByteKey<EpisodeMsgKey>, EpisodeMsg> convertEpisodeObj(
      SpEpisode ep, Long epNumber, ShowMsgKey showKey) {
    EpisodeMsgKey key = EpisodeMsgKey.newBuilder()
        .setEpisodeNumber(epNumber)
        .setShowId(showKey)
        .build();
    EpisodeMsgValue value = EpisodeMsgValue.newBuilder()
        .setDownloadedQuality(0)
        .setLatestLinks(convertUrlList(ep.getDownloads()))
        .setDownloadTime(null)
        .setMessageId(Uuid.randomUUID())
        .build();
    return new KeyValue<>(
        ByteKey.from(key),
        EpisodeMsg.newBuilder().setKey(key).setValue(value).build());
  }

  @Override
  public void close() {

  }

  private List<EpisodeLink> convertUrlList(List<SpEpisodeDownloadEntry> urlList) {
    List<EpisodeLink> output = new ArrayList<>();
    for (SpEpisodeDownloadEntry downloadEntry : urlList) {
      output.add(EpisodeLink.newBuilder()
          .setType(DownloadType.MAGNET)
          .setQuality(downloadEntry.getRes())
          .setUrl(downloadEntry.getMagnet())
          .build());
    }
    return output;
  }

  private String fetchPageFromUrl(String url) throws IOException {
    HtmlPage page = args.getWebClient().getPage(url);
    log.info("Page size: {}", page.getWebResponse().getContentAsString().length());
    args.getWebClient().close();
    return page.getWebResponse().getContentAsString();
  }
}
