package com.brokencircuits.kissad.ui.config;

import com.brokencircuits.kissad.config.TopicAutoconfig;
import com.brokencircuits.kissad.kafka.AdminInterface;
import com.brokencircuits.kissad.kafka.ByteKey;
import com.brokencircuits.kissad.kafka.ClusterConnectionProps;
import com.brokencircuits.kissad.kafka.KeyValueStoreWrapper;
import com.brokencircuits.kissad.kafka.Topic;
import com.brokencircuits.kissad.kafka.TopicMap;
import com.brokencircuits.kissad.messages.ShowMsg;
import com.brokencircuits.kissad.messages.ShowMsgKey;
import com.brokencircuits.messages.KissCaptchaBatchKey;
import com.brokencircuits.messages.KissCaptchaBatchKeywordKey;
import com.brokencircuits.messages.KissCaptchaBatchKeywordMsg;
import com.brokencircuits.messages.KissCaptchaBatchMsg;
import com.brokencircuits.messages.KissCaptchaImgKey;
import com.brokencircuits.messages.KissCaptchaImgMsg;
import com.brokencircuits.messages.KissCaptchaMatchedKeywordMsg;
import java.util.Collection;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(TopicAutoconfig.class)
public class KafkaConfig {

  public static final String STORE_SHOW = "show";
  private static final String STORE_IMG = "images";
  private static final String STORE_CAPTCHA_BATCH = "captcha_batch";

  @Bean
  KeyValueStoreWrapper<ByteKey<KissCaptchaImgKey>, KissCaptchaImgMsg> kissCaptchaImgStore(
      Topic<ByteKey<KissCaptchaImgKey>, KissCaptchaImgMsg> kissCaptchaPictureStoreTopic) {
    return new KeyValueStoreWrapper<>(STORE_IMG, kissCaptchaPictureStoreTopic);
  }

  @Bean
  KeyValueStoreWrapper<ByteKey<ShowMsgKey>, ShowMsg> showStoreWrapper(
      Topic<ByteKey<ShowMsgKey>, ShowMsg> showStoreTopic) {
    return new KeyValueStoreWrapper<>(STORE_SHOW, showStoreTopic);
  }

  @Bean
  KeyValueStoreWrapper<ByteKey<KissCaptchaBatchKey>, KissCaptchaBatchMsg> kissCaptchaBatchStoreWrapper(
      Topic<ByteKey<KissCaptchaBatchKey>, KissCaptchaBatchMsg> kissCaptchaBatchTopic) {
    return new KeyValueStoreWrapper<>(STORE_CAPTCHA_BATCH, kissCaptchaBatchTopic);
  }

  @Bean
  AdminInterface adminInterface(ClusterConnectionProps clusterConnectionProps) throws Exception {
    AdminInterface adminInterface = new AdminInterface(clusterConnectionProps);
    adminInterface.start();
    return adminInterface;
  }

  @Bean
  TopicMap<ByteKey<KissCaptchaBatchKeywordKey>, KissCaptchaBatchKeywordMsg> captchaKeywordMap(
      ClusterConnectionProps clusterConnectionProps,
      Topic<ByteKey<KissCaptchaBatchKeywordKey>, KissCaptchaBatchKeywordMsg> kissCaptchaKeywordTopic) {
    return new TopicMap<>(kissCaptchaKeywordTopic, clusterConnectionProps);
  }

  @Bean
  TopicMap<ByteKey<KissCaptchaBatchKeywordKey>, KissCaptchaMatchedKeywordMsg> captchaMatchedKeywordMap(
      ClusterConnectionProps clusterConnectionProps,
      Topic<ByteKey<KissCaptchaBatchKeywordKey>, KissCaptchaMatchedKeywordMsg> kissCaptchaMatchedKeywordTopic) {
    return new TopicMap<>(kissCaptchaMatchedKeywordTopic, clusterConnectionProps);
  }

  @Bean
  CommandLineRunner autostartKeywordTopicMap(
      Collection<TopicMap<?, ?>> maps) {
    return args -> {
      for (TopicMap<?, ?> map : maps) {
        map.start();
      }
    };
  }
}
