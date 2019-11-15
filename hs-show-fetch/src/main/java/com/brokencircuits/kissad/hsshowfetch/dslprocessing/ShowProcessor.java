package com.brokencircuits.kissad.hsshowfetch.dslprocessing;

import com.brokencircuits.download.messages.DownloadType;
import com.brokencircuits.kissad.Extractor;
import com.brokencircuits.kissad.hsshowfetch.domain.EpisodeDetails;
import com.brokencircuits.kissad.hsshowfetch.domain.MagnetUrl;
import com.brokencircuits.kissad.hsshowfetch.fetch.HsEpisodeFetcher;
import com.brokencircuits.kissad.kafka.Publisher;
import com.brokencircuits.kissad.messages.EpisodeLink;
import com.brokencircuits.kissad.messages.EpisodeMsgKey;
import com.brokencircuits.kissad.messages.EpisodeMsgValue;
import com.brokencircuits.kissad.messages.ShowMsgKey;
import com.brokencircuits.kissad.messages.ShowMsgValue;
import com.brokencircuits.kissad.messages.SourceName;
import com.brokencircuits.kissad.util.Uuid;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.processor.Processor;
import org.apache.kafka.streams.processor.ProcessorContext;

@Slf4j
@RequiredArgsConstructor
public class ShowProcessor implements Processor<ShowMsgKey, ShowMsgValue> {

  private final WebClient webClient;
  private final Extractor<String, Long> showIdExtractor;
  private final HsEpisodeFetcher hsEpisodeFetcher;
  private final Publisher<EpisodeMsgKey, EpisodeMsgValue> episodeMsgPublisher;

  @Override
  public void init(ProcessorContext context) {
  }

  @Override
  public void process(ShowMsgKey key, ShowMsgValue value) {
    log.info("Processing show: {} | {}", key, value);
    String hsUrl = value.getSources().get(SourceName.HORRIBLESUBS.toString());
    try {
      String htmlPage = fetchPageFromUrl(hsUrl);
      Long showId = showIdExtractor.extract(htmlPage);
      Collection<EpisodeDetails> episodes = hsEpisodeFetcher.getEpisodes(showId);

      episodes.forEach(ep -> episodeMsgPublisher.send(convertEpisodeObj(key, ep)));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private KeyValue<EpisodeMsgKey, EpisodeMsgValue> convertEpisodeObj(
      ShowMsgKey key, EpisodeDetails ep) {
    return new KeyValue<>(
        EpisodeMsgKey.newBuilder()
            .setEpisodeNumber(ep.getEpisodeNumber())
            .setShowId(key)
            .build(),
        EpisodeMsgValue.newBuilder()
            .setDownloadedQuality(0)
            .setLatestLinks(convertUrlList(ep.getUrlList()))
            .setDownloadTime(null)
            .setMessageId(Uuid.randomUUID())
            .build());
  }

  @Override
  public void close() {

  }

  private List<EpisodeLink> convertUrlList(List<MagnetUrl> urlList) {
    List<EpisodeLink> output = new ArrayList<>();
    for (MagnetUrl magnetUrl : urlList) {
      output.add(EpisodeLink.newBuilder()
          .setType(DownloadType.MAGNET)
          .setQuality(magnetUrl.getQuality())
          .setUrl(magnetUrl.getUrl())
          .build());
    }
    return output;
  }

  private String fetchPageFromUrl(String url) throws IOException {
    HtmlPage page = webClient.getPage(url);
    log.info("Page size: {}", page.getWebResponse().getContentAsString().length());
    webClient.close();
    return page.getWebResponse().getContentAsString();
  }
}
