package com.brokencircuits.kissad.kissepisodefetch.config;

import com.brokencircuits.kissad.Extractor;
import com.brokencircuits.kissad.config.TopicAutoconfig;
import com.brokencircuits.kissad.kafka.ByteKey;
import com.brokencircuits.kissad.kafka.KeyValueStoreWrapper;
import com.brokencircuits.kissad.kafka.Publisher;
import com.brokencircuits.kissad.kafka.Topic;
import com.brokencircuits.kissad.kissepisodefetch.streams.EpisodeProcessor;
import com.brokencircuits.kissad.kissweb.CaptchaAutoconfig;
import com.brokencircuits.kissad.kissweb.KissWebFetcher;
import com.brokencircuits.kissad.messages.EpisodeMsgKey;
import com.brokencircuits.kissad.messages.KissEpisodeExternalSrcMsg;
import com.brokencircuits.kissad.messages.KissEpisodePageKey;
import com.brokencircuits.kissad.messages.KissEpisodePageMessage;
import com.brokencircuits.kissad.util.SelectCaptchaImg;
import com.brokencircuits.kissad.util.SubmitForm;
import com.brokencircuits.messages.KissCaptchaBatchKeywordKey;
import com.brokencircuits.messages.KissCaptchaMatchedKeywordMsg;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import java.awt.image.BufferedImage;
import java.util.Collection;
import org.apache.kafka.streams.processor.ProcessorSupplier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({TopicAutoconfig.class, CaptchaAutoconfig.class})
public class KafkaConfig {

  @Bean
  ProcessorSupplier<ByteKey<KissEpisodePageKey>, KissEpisodePageMessage> episodeProcessorSupplier(
      KissWebFetcher webFetcher, Extractor<HtmlPage, Collection<BufferedImage>> imageExtractor,
      Extractor<HtmlPage, Collection<String>> keywordExtractor,
      KeyValueStoreWrapper<ByteKey<KissCaptchaBatchKeywordKey>, KissCaptchaMatchedKeywordMsg> matchedKeywordsStore,
      SelectCaptchaImg selectCaptchaImg, SubmitForm submitForm,
      Extractor<HtmlPage, Boolean> isCaptchaPageChecker,
      Extractor<HtmlPage, String> episodeIframeExtractor,
      Publisher<ByteKey<EpisodeMsgKey>, KissEpisodeExternalSrcMsg> externalSrcMsgPublisher) {
    return () -> new EpisodeProcessor(webFetcher, imageExtractor, keywordExtractor,
        matchedKeywordsStore, selectCaptchaImg, submitForm, isCaptchaPageChecker,
        episodeIframeExtractor, externalSrcMsgPublisher);
  }

  @Bean
  KeyValueStoreWrapper<ByteKey<KissCaptchaBatchKeywordKey>, KissCaptchaMatchedKeywordMsg> matchedKeywordsStore(
      Topic<ByteKey<KissCaptchaBatchKeywordKey>, KissCaptchaMatchedKeywordMsg> kissCaptchaMatchedKeywordTopic) {
    return new KeyValueStoreWrapper<>("matched-keywords", kissCaptchaMatchedKeywordTopic);
  }
//
//  @Bean
//  Future<?> testRunner(
//      ProcessorSupplier<ByteKey<KissEpisodePageKey>, KissEpisodePageMessage> episodeProcessorSupplier,
//      TaskScheduler taskScheduler) {
//    return taskScheduler.schedule(() -> {
//      Processor<ByteKey<KissEpisodePageKey>, KissEpisodePageMessage> processor = episodeProcessorSupplier
//          .get();
//
//      KissEpisodePageKey key = KissEpisodePageKey.newBuilder()
//          .setShowKey(ShowMsgKey.newBuilder()
//              .setShowId(Uuid.fromString("64746083-33c0-43ea-8034-ba1116c823a9"))
//              .build())
//          .setEpisodeName("Full-Time Magister 4th Season Episode 001")
//          .build();
//      KissEpisodePageMessage msg = KissEpisodePageMessage.newBuilder()
//          .setKey(key)
//          .setUrl("https://kissanime.ru/Anime/Full-Time-Magister-4th-Season/Episode-001?id=169645")
//          .setEpisodeName("Full-Time Magister 4th Season Episode 001")
//          .setEpisodeNumber(1)
//          .setRetrieveTime(Instant.now())
//          .build();
//      processor.process(new ByteKey<>(key), msg);
//    }, Instant.now());
//  }
}
