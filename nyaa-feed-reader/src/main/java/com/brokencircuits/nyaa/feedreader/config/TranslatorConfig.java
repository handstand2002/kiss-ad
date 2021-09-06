package com.brokencircuits.nyaa.feedreader.config;

import com.brokencircuits.nyaa.feedreader.domain.DownloadLink;
import com.brokencircuits.nyaa.feedreader.domain.Translator;
import com.sun.syndication.feed.synd.SyndEntry;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jdom.Element;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TranslatorConfig {

  @Bean
  Translator<SyndEntry, DownloadLink> translator() {
    return input -> {
      Map<String, Object> foreignMarkup = new HashMap<>();
      ((List<Element>) input.getForeignMarkup())
          .forEach(m -> foreignMarkup.put(m.getName(), m.getValue()));

      return DownloadLink.builder()
          .title(input.getTitle())
          .link(input.getLink())
          .guid(input.getUri())
          .pubDate(ZonedDateTime.ofInstant(input.getPublishedDate().toInstant(),
              ZoneId.systemDefault()))
          .seeders(getProperty("seeders", foreignMarkup, Integer.class).orElse(-1))
          .leechers(getProperty("leechers", foreignMarkup, Integer.class).orElse(-1))
          .downloads(getProperty("downloads", foreignMarkup, Integer.class).orElse(-1))
          .infoHash(getProperty("infoHash", foreignMarkup, String.class).orElse(null))
          .categoryId(getProperty("categoryId", foreignMarkup, String.class).orElse(null))
          .category(getProperty("category", foreignMarkup, String.class).orElse(null))
          .size(getProperty("size", foreignMarkup, String.class).orElse(null))
          .comments(getProperty("comments", foreignMarkup, Integer.class).orElse(-1))
          .trusted(getProperty("trusted", foreignMarkup, Boolean.class).orElse(false))
          .remake(getProperty("remake", foreignMarkup, Boolean.class).orElse(false))
          .build();
    };
  }

  private <T> Optional<T> getProperty(String key, Map<String, Object> foreignMarkup,
                                      Class<T> forClass) {
    Object o = foreignMarkup.get(key);
    Constructor<T> constructor;
    try {
      constructor = forClass.getConstructor(String.class);
      return Optional.of(constructor.newInstance(String.valueOf(o)));
    } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
      e.printStackTrace();
    }
    return Optional.empty();
  }
}
