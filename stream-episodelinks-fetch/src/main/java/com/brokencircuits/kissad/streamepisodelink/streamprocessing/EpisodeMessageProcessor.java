package com.brokencircuits.kissad.streamepisodelink.streamprocessing;

import com.brokencircuits.kissad.kafka.Publisher;
import com.brokencircuits.kissad.kissweb.KissWebFetcher;
import com.brokencircuits.kissad.messages.DownloadAvailability;
import com.brokencircuits.kissad.messages.ExternalEpisodeLinkMessage;
import com.brokencircuits.kissad.messages.KissEpisodePageKey;
import com.brokencircuits.kissad.messages.KissEpisodePageMessage;
import com.brokencircuits.kissad.messages.VideoSource;
import com.gargoylesoftware.htmlunit.html.DomNode;
import com.gargoylesoftware.htmlunit.html.DomNodeList;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.processor.Processor;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EpisodeMessageProcessor implements
    Processor<ByteKey<KissEpisodePageKey>, KissEpisodePageMessage> {

  final private static String myVideoDivSelector = "#divMyVideo";

  final private Publisher<ByteKey<KissEpisodePageKey>, ExternalEpisodeLinkMessage> episodeLinkPublisher;
  final private KissWebFetcher webFetcher;

  @Value("${messaging.stores.download-availability}")
  private String downloadAvailabilityStoreName;
  private ReadOnlyKeyValueStore<String, DownloadAvailability> availabilityStore;

  @Override
  public void init(ProcessorContext context) {
    availabilityStore = (ReadOnlyKeyValueStore<String, DownloadAvailability>) context
        .getStateStore(downloadAvailabilityStoreName);
  }

  @Override
  public void process(KissEpisodePageKey key, KissEpisodePageMessage msg) {
    if (!downloaderIsAvailable()) {
      log.info("Waiting until downloader is available before processing more");
      while (!downloaderIsAvailable()) {
        try {
          Thread.sleep(30000);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }

    log.info("Processing {} | {}", key, msg);

    String newUrl = msg.getUrl() + "&s=rapidvideo";
    try {
      HtmlPage htmlPage = webFetcher.fetchPage(newUrl);

      log.info("Page:\n{}", htmlPage.getTextContent());
      DomNodeList<DomNode> myVideoDivList = htmlPage.querySelectorAll(myVideoDivSelector);
      log.info("Found {} nodes matching {}", myVideoDivList.size(), myVideoDivSelector);
      List<DomNode> iframeNodes = new ArrayList<>();
      for (DomNode node : myVideoDivList) {
        DomNodeList<DomNode> childNodes = node.querySelectorAll("iframe");
        log.info("Found {} iframe nodes within div node {}", childNodes.size(), node);
        iframeNodes.addAll(childNodes);
      }

      log.info("Collected {} potential iframes", iframeNodes.size());

      for (DomNode iframe : iframeNodes) {
        String iframeSrc = iframe.getAttributes().getNamedItem("src").getTextContent();
        if (iframeSrc.toLowerCase().contains("rapidvideo.com")) {
          log.info("Found rapidvideo iframe:\n{}", iframe.asXml());

          ExternalEpisodeLinkMessage externalLinkMessage = ExternalEpisodeLinkMessage.newBuilder()
              .setEpisodeName(msg.getEpisodeName())
              .setEpisodeNumber(msg.getEpisodeNumber())
              .setRetrieveTime(DateTime.now())
              .setSeasonNumber(msg.getSeasonNumber())
              .setShowName(msg.getShowName())
              .setSubOrDub(msg.getSubOrDub())
              .setUrl(iframeSrc)
              .setVideoSource(VideoSource.RAPIDVIDEO)
              .build();

          episodeLinkPublisher.send(key, externalLinkMessage);
          break;
        }
      }

    } catch (IOException e) {
      e.printStackTrace();
    }

  }

  @Override
  public void close() {

  }

  private boolean downloaderIsAvailable() {
    KeyValueIterator<String, DownloadAvailability> iterator = availabilityStore.all();
    while (iterator.hasNext()) {
      KeyValue<String, DownloadAvailability> entry = iterator.next();
      if (entry.value.getAvailableCapacity() > 0) {
        return true;
      }
    }
    return false;
  }
}
