package com.brokencircuits.kissad.streamshowfetch.extractor;

import com.brokencircuits.kissad.Extractor;
import com.brokencircuits.kissad.messages.KissEpisodePageKey;
import com.brokencircuits.kissad.messages.KissEpisodePageMessage;
import com.brokencircuits.kissad.messages.KissShowMessage;
import com.brokencircuits.kissad.messages.SubOrDub;
import com.gargoylesoftware.htmlunit.html.DomNode;
import com.gargoylesoftware.htmlunit.html.DomNodeList;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.KeyValue;
import org.joda.time.DateTime;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EpisodeMessageExtractor implements
    Extractor<KeyValue<KissShowMessage, HtmlPage>, List<KeyValue<KissEpisodePageKey, KissEpisodePageMessage>>> {

  private static final Pattern findEpisodeNumberPattern = Pattern.compile("Episode ([0-9]+)");
  private static final Pattern findSubOrDubPattern = Pattern
      .compile("\\((Sub)\\)", Pattern.CASE_INSENSITIVE);

  @Override
  public List<KeyValue<KissEpisodePageKey, KissEpisodePageMessage>> extract(
      KeyValue<KissShowMessage, HtmlPage> inputPair) throws URISyntaxException {

    HtmlPage htmlPage = inputPair.value;
    KissShowMessage showMessage = inputPair.key;

    URI uri = new URI(showMessage.getUrl());
    String rootPage = uri.getScheme() + "://" + uri.getHost();

    log.info("Retrieved page:\n{}", htmlPage.asXml());
    DomNodeList<DomNode> nodes = htmlPage.getBody()
        .querySelectorAll(".barContent.episodeList table.listing");

    if (nodes.isEmpty()) {
      throw new IllegalArgumentException("Unable to find episode list table in page: "
          + htmlPage.asXml());
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
        log.error("Unable to create episodePageMessage: {} | {}", link.asXml(), e.getMessage());
      }
    });

    // sort by episode number
    pageList.sort((o1, o2) -> {
      if (o1.value.getEpisodeNumber() != null && o2.value.getEpisodeNumber() != null) {
        return o1.value.getEpisodeNumber().compareTo(o2.value.getEpisodeNumber());
      } else if (o1.value.getEpisodeNumber() == null) {
        return -1;
      } else if (o2.value.getEpisodeNumber() == null) {
        return 1;
      }
      return 0;
    });

    return pageList;
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
}
