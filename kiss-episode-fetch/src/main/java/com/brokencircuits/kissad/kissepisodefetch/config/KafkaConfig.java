package com.brokencircuits.kissad.kissepisodefetch.config;

import com.brokencircuits.kissad.Extractor;
import com.brokencircuits.kissad.Translator;
import com.brokencircuits.kissad.config.TopicAutoconfig;
import com.brokencircuits.kissad.kafka.ByteKey;
import com.brokencircuits.kissad.kafka.ClusterConnectionProps;
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
import com.brokencircuits.messages.KissCaptchaBatchKeywordMsg;
import com.brokencircuits.messages.KissCaptchaImgKey;
import com.brokencircuits.messages.KissCaptchaImgMsg;
import com.brokencircuits.messages.KissCaptchaMatchedKeywordMsg;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import java.awt.image.BufferedImage;
import java.util.Collection;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.processor.ProcessorSupplier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({TopicAutoconfig.class, CaptchaAutoconfig.class})
public class KafkaConfig {

  @Bean
  Publisher<ByteKey<KissCaptchaBatchKeywordKey>, KissCaptchaBatchKeywordMsg> kissCaptchaKeywordPublisher(
      Topic<ByteKey<KissCaptchaBatchKeywordKey>, KissCaptchaBatchKeywordMsg> kissCaptchaKeywordTopic,
      ClusterConnectionProps clusterProps) {
    return new Publisher<>(clusterProps.asProperties(), kissCaptchaKeywordTopic);
  }

  @Bean
  Publisher<ByteKey<KissCaptchaImgKey>, KissCaptchaImgMsg> kissCaptchaPictureMsgPublisher(
      Topic<ByteKey<KissCaptchaImgKey>, KissCaptchaImgMsg> kissCaptchaPictureStoreTopic,
      ClusterConnectionProps clusterProps) {
    return new Publisher<>(clusterProps.asProperties(), kissCaptchaPictureStoreTopic);
  }

  @Bean
  ProcessorSupplier<ByteKey<KissEpisodePageKey>, KissEpisodePageMessage> episodeProcessorSupplier(
      KissWebFetcher webFetcher, Extractor<HtmlPage, Collection<BufferedImage>> imageExtractor,
      Extractor<HtmlPage, Collection<String>> keywordExtractor,
      KeyValueStoreWrapper<ByteKey<KissCaptchaBatchKeywordKey>, KissCaptchaMatchedKeywordMsg> matchedKeywordsStore,
      SelectCaptchaImg selectCaptchaImg, SubmitForm submitForm,
      Extractor<HtmlPage, Boolean> isCaptchaPageChecker,
      Extractor<HtmlPage, String> episodeIframeExtractor,
      Publisher<ByteKey<EpisodeMsgKey>, KissEpisodeExternalSrcMsg> externalSrcMsgPublisher,
      Publisher<ByteKey<KissCaptchaImgKey>, KissCaptchaImgMsg> kissCaptchaPictureMsgPublisher,
      Publisher<ByteKey<KissCaptchaBatchKeywordKey>, KissCaptchaBatchKeywordMsg> kissCaptchaKeywordPublisher,
      Translator<BufferedImage, KeyValue<ByteKey<KissCaptchaImgKey>, KissCaptchaImgMsg>> imgToMsgTranslator) {
    return () -> new EpisodeProcessor(webFetcher, imageExtractor, keywordExtractor,
        matchedKeywordsStore, selectCaptchaImg, submitForm, isCaptchaPageChecker,
        episodeIframeExtractor, externalSrcMsgPublisher, kissCaptchaPictureMsgPublisher,
        kissCaptchaKeywordPublisher, imgToMsgTranslator);
  }

  @Bean
  KeyValueStoreWrapper<ByteKey<KissCaptchaBatchKeywordKey>, KissCaptchaMatchedKeywordMsg> matchedKeywordsStore(
      Topic<ByteKey<KissCaptchaBatchKeywordKey>, KissCaptchaMatchedKeywordMsg> kissCaptchaMatchedKeywordTopic) {
    return new KeyValueStoreWrapper<>("matched-keywords", kissCaptchaMatchedKeywordTopic);
  }

}
