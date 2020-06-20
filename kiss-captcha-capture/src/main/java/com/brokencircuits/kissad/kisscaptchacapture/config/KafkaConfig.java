package com.brokencircuits.kissad.kisscaptchacapture.config;

import com.brokencircuits.kissad.Translator;
import com.brokencircuits.kissad.config.TopicAutoconfig;
import com.brokencircuits.kissad.kafka.AdminInterface;
import com.brokencircuits.kissad.kafka.ByteKey;
import com.brokencircuits.kissad.kafka.ClusterConnectionProps;
import com.brokencircuits.kissad.kafka.Publisher;
import com.brokencircuits.kissad.kafka.Topic;
import com.brokencircuits.kissad.kissweb.KissWebFetcher;
import com.brokencircuits.kissad.util.Uuid;
import com.brokencircuits.messages.Command;
import com.brokencircuits.messages.KissCaptchaBatchKey;
import com.brokencircuits.messages.KissCaptchaBatchKeywordKey;
import com.brokencircuits.messages.KissCaptchaBatchKeywordMsg;
import com.brokencircuits.messages.KissCaptchaBatchMsg;
import com.brokencircuits.messages.KissCaptchaImgKey;
import com.brokencircuits.messages.KissCaptchaImgMsg;
import com.gargoylesoftware.htmlunit.WebResponse;
import com.gargoylesoftware.htmlunit.html.DomNode;
import com.gargoylesoftware.htmlunit.html.DomNodeList;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.KeyValue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Slf4j
@Configuration
@Import(TopicAutoconfig.class)
public class KafkaConfig {

  @Bean
  @ConfigurationProperties(prefix = "messaging")
  ClusterConnectionProps clusterConnectionProps() {
    return new ClusterConnectionProps();
  }

  @Bean
  Publisher<ByteKey<KissCaptchaImgKey>, KissCaptchaImgMsg> kissCaptchaPictureMsgPublisher(
      Topic<ByteKey<KissCaptchaImgKey>, KissCaptchaImgMsg> kissCaptchaPictureStoreTopic,
      ClusterConnectionProps clusterProps) {
    return new Publisher<>(clusterProps.asProperties(), kissCaptchaPictureStoreTopic);
  }

  @Bean
  Publisher<ByteKey<KissCaptchaBatchKey>, KissCaptchaBatchMsg> kissCaptchaBatchPublisher(
      Topic<ByteKey<KissCaptchaBatchKey>, KissCaptchaBatchMsg> kissCaptchaBatchTopic,
      ClusterConnectionProps clusterProps) {
    return new Publisher<>(clusterProps.asProperties(), kissCaptchaBatchTopic);
  }

  @Bean
  Publisher<ByteKey<KissCaptchaBatchKeywordKey>, KissCaptchaBatchKeywordMsg> kissCaptchaKeywordPublisher(
      Topic<ByteKey<KissCaptchaBatchKeywordKey>, KissCaptchaBatchKeywordMsg> kissCaptchaKeywordTopic,
      ClusterConnectionProps clusterProps) {
    return new Publisher<>(clusterProps.asProperties(), kissCaptchaKeywordTopic);
  }


  @Bean
  Function<HtmlPage, Collection<BufferedImage>> imageExtractor(KissWebFetcher webFetcher,
      @Value("${kiss.captcha.base-url}") String baseUrl,
      @Value("${kiss.captcha.all-img-css-selector}") String imgSelector) {
    return page -> {
      DomNodeList<DomNode> imgNodes = page.getBody().querySelectorAll(imgSelector);

      Set<BufferedImage> batchImages = new HashSet<>();
      for (int i = 0; i < imgNodes.size(); i++) {
        DomNode imgNode = imgNodes.get(i);
        try {
          WebResponse imgResponse = webFetcher.fetchResource(
              baseUrl + imgNode.getAttributes().getNamedItem("src").getTextContent());
          BufferedImage img = ImageIO.read(imgResponse.getContentAsStream());
          img = resizeImage(img, 100, 100);
          batchImages.add(img);

        } catch (IOException e) {
          log.warn("Unable to fetch image for node {}", imgNode);
        }
      }

      return batchImages;
    };
  }

  @Bean
  Function<HtmlPage, Collection<String>> keywordExtractor(
      @Value("${kiss.captcha.all-keyword-css-selector}") String keywordSelector) {
    return page -> {
      DomNodeList<DomNode> keywordNodes = page.getBody().querySelectorAll(keywordSelector);

      Set<String> keywords = new HashSet<>();
      for (DomNode keywordNode : keywordNodes) {
        keywords.add(keywordNode.getFirstChild().asText());
      }
      return keywords;
    };
  }

  @Bean
  AdminInterface adminInterface(
      @Value("${kiss.captcha.url}") String kissCaptchaUrl, ClusterConnectionProps props,
      Publisher<ByteKey<KissCaptchaImgKey>, KissCaptchaImgMsg> kissCaptchaPicturePublisher,
      Publisher<ByteKey<KissCaptchaBatchKey>, KissCaptchaBatchMsg> kissCaptchaBatchPublisher,
      Publisher<ByteKey<KissCaptchaBatchKeywordKey>, KissCaptchaBatchKeywordMsg> kissCaptchaKeywordPublisher,
      KissWebFetcher webFetcher, Function<HtmlPage, Collection<BufferedImage>> imageExtractor,
      Function<HtmlPage, Collection<String>> keywordExtractor,
      Translator<BufferedImage, KeyValue<ByteKey<KissCaptchaImgKey>, KissCaptchaImgMsg>> imgToMsgTranslator)
      throws Exception {

    AdminInterface adminInterface = new AdminInterface(props);
    adminInterface.registerCommand(Command.QUERY_CAPTCHA_PICS, command -> {

      List<String> parameters = command.getValue().getParameters();
      long numLoads = Long.parseLong(parameters.get(0));

      log.info("Need to load page {} times", numLoads);
      try {
        HtmlPage htmlPage = webFetcher.fetchPage(kissCaptchaUrl);

        log.info("Retrieved page: {}", htmlPage);

        Collection<BufferedImage> images = imageExtractor.apply(htmlPage);
        Collection<String> keywords = keywordExtractor.apply(htmlPage);
        keywords.forEach(keyword -> log.info("Keyword: {}", keyword));
        images.forEach(image -> log.info("Image: {}", image));

        Set<KeyValue<ByteKey<KissCaptchaImgKey>, KissCaptchaImgMsg>> messages = new HashSet<>();
        images.forEach(img -> messages.add(imgToMsgTranslator.translate(img)));

        Uuid batchId = Uuid.randomUUID();

        keywords.forEach(phrase -> {
          KissCaptchaBatchKeywordKey key = KissCaptchaBatchKeywordKey.newBuilder()
              .setBatchId(batchId).setKeyword(phrase)
              .build();
          ByteKey<KissCaptchaBatchKeywordKey> byteKey = new ByteKey<>(key);
          KissCaptchaBatchKeywordMsg keywordMsg = KissCaptchaBatchKeywordMsg.newBuilder()
              .setImageKeys(imageKeys(messages)).setKey(key).build();
          kissCaptchaKeywordPublisher.send(byteKey, keywordMsg);
        });

        messages.forEach(kissCaptchaPicturePublisher::send);

      } catch (IOException e) {
        log.error("Could not retrieve resource", e);
      }


    });
    adminInterface.start();
    return adminInterface;
  }

  private List<KissCaptchaImgKey> imageKeys(
      Set<KeyValue<ByteKey<KissCaptchaImgKey>, KissCaptchaImgMsg>> msgList) {
    return msgList.stream().map(kvPair -> kvPair.value.getKey()).collect(Collectors.toList());
  }

  private static BufferedImage resizeImage(BufferedImage originalImage, int newWidth,
      int newHeight) {
    BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, originalImage.getType());
    Graphics2D g = resizedImage.createGraphics();
    g.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
    g.dispose();

    return resizedImage;
  }

//  @Bean
//  CommandLineRunner testRun(AdminInterface adminInterface,
//      ClusterConnectionProps clusterConnectionProps) {
//    return args -> {
//
//      List<String> parameters = new ArrayList<>();
//      parameters.add("1");
//      String applicationId = clusterConnectionProps.asProperties().getProperty("application.id");
//
//      AdminCommandMsg command = AdminCommandMsg.newBuilder()
//          .setKey(AdminCommandKey.newBuilder().setCommandId(Uuid.randomUUID())
//              .setApplicationId(applicationId).build())
//          .setValue(AdminCommandValue.newBuilder()
//              .setSendTime(Instant.now())
//              .setCommand(Command.QUERY_CAPTCHA_PICS)
//              .setParameters(parameters)
//              .build())
//          .build();
//      adminInterface.handleCommandManual(command);
//    };
//  }
}
