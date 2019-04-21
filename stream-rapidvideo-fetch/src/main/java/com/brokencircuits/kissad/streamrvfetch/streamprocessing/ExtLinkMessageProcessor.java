package com.brokencircuits.kissad.streamrvfetch.streamprocessing;

import com.brokencircuits.kissad.WebFetcher;
import com.brokencircuits.kissad.kafka.Publisher;
import com.brokencircuits.kissad.messages.DownloadLink;
import com.brokencircuits.kissad.messages.ExternalDownloadLinkKey;
import com.brokencircuits.kissad.messages.ExternalDownloadLinkMessage;
import com.brokencircuits.kissad.messages.ExternalEpisodeLinkMessage;
import com.brokencircuits.kissad.messages.KissEpisodePageKey;
import com.gargoylesoftware.htmlunit.html.DomNode;
import com.gargoylesoftware.htmlunit.html.DomNodeList;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.processor.Processor;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.joda.time.DateTime;
import org.springframework.stereotype.Component;
import org.w3c.dom.NamedNodeMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExtLinkMessageProcessor implements
    Processor<KissEpisodePageKey, ExternalEpisodeLinkMessage> {

  final private WebFetcher webFetcher;
  final private Publisher<ExternalDownloadLinkKey, ExternalDownloadLinkMessage> episodeLinkPublisher;

  @Override
  public void init(ProcessorContext processorContext) {

  }

  @Override
  public void process(KissEpisodePageKey key, ExternalEpisodeLinkMessage msg) {
    log.info("Processing {} | {}", key, msg);
    try {
      HtmlPage page = webFetcher.fetchPage(msg.getUrl());
      log.info("Page:\n{}", page.asXml());

      DomNodeList<DomNode> videoSourceNodes = page.querySelectorAll("div video#videojs source");

      ExternalDownloadLinkKey downloadLinkKey = ExternalDownloadLinkKey.newBuilder()
          .setEpisodeName(key.getEpisodeName())
          .setShowName(key.getShowName())
          .setVideoSource(msg.getVideoSource())
          .build();

      ExternalDownloadLinkMessage downloadLinkMsg = ExternalDownloadLinkMessage.newBuilder()
          .setEpisodeNumber(msg.getEpisodeNumber())
          .setLinks(new ArrayList<>())
          .setRetrieveTime(DateTime.now())
          .setSeasonNumber(msg.getSeasonNumber())
          .setSubOrDub(msg.getSubOrDub())
          .build();

      for (DomNode source : videoSourceNodes) {
        NamedNodeMap attr = source.getAttributes();
        DownloadLink link = DownloadLink.newBuilder()
            .setResolution(Integer.parseInt(attr.getNamedItem("data-res").getTextContent()))
            .setUrl(attr.getNamedItem("src").getTextContent())
            .build();
        downloadLinkMsg.getLinks().add(link);
      }

      episodeLinkPublisher.send(downloadLinkKey, downloadLinkMsg);

    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void close() {

  }
}
