package com.brokencircuits.kissad.streamshowfetch.streamprocessing;

import com.brokencircuits.kissad.kafka.Publisher;
import com.brokencircuits.kissad.kissweb.KissWebFetcher;
import com.brokencircuits.kissad.messages.KissEpisodePageKey;
import com.brokencircuits.kissad.messages.KissEpisodePageMessage;
import com.brokencircuits.kissad.messages.KissShowMessage;
import com.brokencircuits.kissad.messages.SubOrDub;
import com.gargoylesoftware.htmlunit.html.DomNode;
import com.gargoylesoftware.htmlunit.html.DomNodeList;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.processor.Processor;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.joda.time.DateTime;
import org.springframework.stereotype.Component;
import sun.misc.Regexp;

@Slf4j
@Component
@RequiredArgsConstructor
public class ShowMessageProcessor implements Processor<Long, KissShowMessage> {

  private static final Pattern findEpisodeNumberPattern = Pattern.compile("Episode ([0-9]+)");
  private static final Pattern findSubOrDubPattern = Pattern
      .compile("\\((Sub)\\)", Pattern.CASE_INSENSITIVE);
  final private KissWebFetcher webFetcher;
  final private Publisher<KissEpisodePageKey, KissEpisodePageMessage> episodeMessagePublisher;

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

      URI uri = new URI(url);
      String rootPage = uri.getScheme() + "://" + uri.getHost();

      log.info("Retrieved page:\n{}", htmlPage.asXml());
      DomNodeList<DomNode> nodes = htmlPage.getBody()
          .querySelectorAll(".barContent.episodeList table.listing");

      if (nodes.isEmpty()) {
        log.error("Unable to find episodeList on page");
        return;
      }
      DomNode tableNode = nodes.get(0);
      DomNodeList<DomNode> links = tableNode.querySelectorAll("tr td a");

      log.info("Found {} kiss episode links", links.size());

      List<KeyValue<KissEpisodePageKey, KissEpisodePageMessage>> pageList = new ArrayList<>();
      links.forEach(link -> {
        String episodeName = link.getTextContent().trim();
        String episodeUrl = link.getAttributes().getNamedItem("href").getTextContent();

        Integer episodeNumber = extractEpisodeNumberFromName(episodeName);
        try {
          KissEpisodePageKey pageKey = KissEpisodePageKey.newBuilder()
              .setEpisodeName(episodeName)
              .setShowName(showMessage.getName())
              .build();

          KissEpisodePageMessage pageMessage = KissEpisodePageMessage.newBuilder()
              .setRetrieveTime(DateTime.now())
              .setUrl(rootPage + episodeUrl)
              .setEpisodeNumber(episodeNumber)
              .setSubOrDub(extractSubOrDubFromName(episodeName))
              .setEpisodeName(episodeName)
              .setShowName(showMessage.getName())
              .setSeasonNumber(showMessage.getSeasonNumber())
              .build();

          pageList.add(new KeyValue<>(pageKey, pageMessage));

        } catch (NullPointerException e) {
          log.info("Unable to create episodePageMessage: {} | {}", link.asXml(), e.getMessage());
        }
      });

      // sort by episode number
      pageList.sort(Comparator.comparing(o -> o.value.getEpisodeNumber()));

      // send all messages in order
      pageList.forEach(episodeMessagePublisher::send);

    } catch (IOException | URISyntaxException e1) {
      e1.printStackTrace();
    }
  }

  private SubOrDub extractSubOrDubFromName(String episodeName) {
    Matcher matcher = findSubOrDubPattern.matcher(episodeName);
    if (matcher.find()) {
      return SubOrDub.valueOf(matcher.group(1).toUpperCase());
    }
    return null;
  }

  private Integer extractEpisodeNumberFromName(String episodeName) {
    Matcher matcher = findEpisodeNumberPattern.matcher(episodeName);
    if (matcher.find()) {
      return Integer.parseInt(matcher.group(1));
    }
    return null;
  }

  @Override
  public void close() {

  }
}
