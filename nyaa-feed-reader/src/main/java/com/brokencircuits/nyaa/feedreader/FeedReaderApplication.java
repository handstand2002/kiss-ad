package com.brokencircuits.nyaa.feedreader;

import com.brokencircuits.nyaa.feedreader.domain.DownloadLink;
import com.brokencircuits.nyaa.feedreader.domain.Translator;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Slf4j
@SpringBootApplication
@EnableScheduling
@RequiredArgsConstructor
public class FeedReaderApplication {

  public static void main(String[] args) {
    SpringApplication.run(FeedReaderApplication.class, args);
  }

  @Value("${rss-feed.url}")
  private String url;
  private String latestInfoHash = null;
  private final Translator<SyndEntry, DownloadLink> translator;

  @Scheduled(cron = "0/5 * * * * *")
  public void check() throws IOException, FeedException {

    URL feedSource = new URL(url);
    SyndFeedInput input = new SyndFeedInput();
    SyndFeed feed = input.build(new XmlReader(feedSource));
    List<SyndEntry> entries = feed.getEntries();
    boolean firstEntry = true;
    for (SyndEntry entry : entries) {
      DownloadLink e = translator.apply(entry);
      if (Objects.equals(e.getInfoHash(), latestInfoHash)) {
        break;
      }
      if (firstEntry) {
        latestInfoHash = e.getInfoHash();
        firstEntry = false;
      }
      log.info("New Entry: {}", e);
    }
  }

//  @Bean
//  CommandLineRunner runner(@Value("${rss-feed.url}") String url,
//      Translator<SyndEntry, DownloadLink> translator) throws IOException {
//    return args -> {
//
//      URL feedSource = new URL(url);
//      SyndFeedInput input = new SyndFeedInput();
//      SyndFeed feed = input.build(new XmlReader(feedSource));
//      List<SyndEntry> entries = feed.getEntries();
//      entries.stream().map(translator::apply)
//          .forEach(e -> {
//            log.info("Entry: {}", e);
//          });
//
//      log.info("Test");
//
//    };
//  }
}
