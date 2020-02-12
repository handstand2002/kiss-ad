package com.brokencircuits.kissad.kisscaptchacapture.config;

import com.brokencircuits.kissad.Translator;
import com.brokencircuits.kissad.kafka.ByteKey;
import com.brokencircuits.messages.KissCaptchaImgKey;
import com.brokencircuits.messages.KissCaptchaImgMsg;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import javax.imageio.ImageIO;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.KeyValue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class TranslatorConfig {

  private final static String IMG_FORMAT = "bmp";

  @Bean
  Translator<BufferedImage, KeyValue<ByteKey<KissCaptchaImgKey>, KissCaptchaImgMsg>> imgToMsgTranslator() {
    return img -> {
      try {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ImageIO.write(img, IMG_FORMAT, os);

        ByteBuffer imgBytes = ByteBuffer.wrap(os.toByteArray());

        KissCaptchaImgKey key = KissCaptchaImgKey.newBuilder()
            .setImgHash(String.valueOf(imgBytes.hashCode())).build();

        KissCaptchaImgMsg value = KissCaptchaImgMsg.newBuilder().setKey(key).setImgBytes(imgBytes)
            .build();

        return KeyValue.pair(ByteKey.from(key), value);
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    };
  }
}
