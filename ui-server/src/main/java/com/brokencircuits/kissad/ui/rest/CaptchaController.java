package com.brokencircuits.kissad.ui.rest;

import com.brokencircuits.kissad.kafka.AdminInterface;
import com.brokencircuits.kissad.kafka.ByteKey;
import com.brokencircuits.kissad.kafka.KeyValueStoreWrapper;
import com.brokencircuits.kissad.kafka.TopicMap;
import com.brokencircuits.kissad.ui.rest.domain.CaptchaSubmission;
import com.brokencircuits.messages.Command;
import com.brokencircuits.messages.KissCaptchaBatchKeywordKey;
import com.brokencircuits.messages.KissCaptchaBatchKeywordMsg;
import com.brokencircuits.messages.KissCaptchaImgKey;
import com.brokencircuits.messages.KissCaptchaImgMsg;
import com.brokencircuits.messages.KissCaptchaMatchedKeywordMsg;
import com.google.common.io.ByteStreams;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Slf4j
@Controller
@RequiredArgsConstructor
public class CaptchaController {

  private final KeyValueStoreWrapper<ByteKey<KissCaptchaImgKey>, KissCaptchaImgMsg> kissCaptchaImgStore;
  private final TopicMap<ByteKey<KissCaptchaBatchKeywordKey>, KissCaptchaBatchKeywordMsg> captchaKeywordMap;
  private final TopicMap<ByteKey<KissCaptchaBatchKeywordKey>, KissCaptchaMatchedKeywordMsg> captchaMatchedKeywordMap;
  private final AdminInterface adminInterface;
  @Value("${kiss.captcha.request.application-id}")
  private String captchaCaptureId;

  private void requestNewCaptcha() {
    adminInterface.sendCommand(captchaCaptureId, Command.QUERY_CAPTCHA_PICS, "1");
  }

  @RequestMapping(value = "/captcha", method = RequestMethod.GET)
  public void doCaptcha(Model model) throws InterruptedException {

    Entry<ByteKey<KissCaptchaBatchKeywordKey>, KissCaptchaBatchKeywordMsg> entry = firstValidKeywordEntry();
    if (entry == null) {
      requestAndWaitForCaptcha();
      entry = firstValidKeywordEntry();
    }
    if (entry == null) {
      return;
    }

    model.addAttribute("batchId", entry.getValue().getKey().getBatchId());
    model.addAttribute("keyword", entry.getValue().getKey().getKeyword());
    model.addAttribute("images",
        entry.getValue().getImageKeys().stream().map(KissCaptchaImgKey::getImgHash)
            .collect(Collectors.toList()));
  }

  private Entry<ByteKey<KissCaptchaBatchKeywordKey>, KissCaptchaBatchKeywordMsg> firstValidKeywordEntry() {
    boolean foundValidEntry = false;
    Entry<ByteKey<KissCaptchaBatchKeywordKey>, KissCaptchaBatchKeywordMsg> entry = null;
    while (!foundValidEntry && captchaKeywordMap.size() > 0) {
      entry = captchaKeywordMap.entrySet().iterator().next();
      foundValidEntry = entry.getValue().getImageKeys().stream()
          .allMatch(key -> kissCaptchaImgStore.get(imageKey(key.getImgHash())) != null);
      if (!foundValidEntry) {
        captchaKeywordMap.remove(entry.getKey());
        entry = null;
      }
    }
    return entry;
  }

  private void requestAndWaitForCaptcha() throws InterruptedException {
    requestNewCaptcha();
    log.info("Requesting captcha");
    Instant requestTime = Instant.now();
    while (captchaKeywordMap.size() == 0 && Instant.now().isBefore(requestTime.plusSeconds(30))) {
      Thread.sleep(100);
    }
    if (captchaKeywordMap.size() > 0) {
      log.info("Received captcha");
      // wait for the global table with images to be updated
      Thread.sleep(1000);
    } else {
      log.info("Timed out waiting for captcha");
    }
  }

  private ByteKey<KissCaptchaImgKey> imageKey(String imgHash) {
    return new ByteKey<>(KissCaptchaImgKey.newBuilder().setImgHash(imgHash).build());
  }

  @RequestMapping(value = "/captcha-submit", method = RequestMethod.POST)
  public String submitCaptcha(CaptchaSubmission submission, Model model) {

    if (submission.getBatchId() == null || submission.getHash() == null
        || submission.getKeyword() == null) {
      return "redirect:/captcha";
    }

    // look up image based on keyword and batchID, get the perceptual hash
    KissCaptchaImgKey key = KissCaptchaImgKey.newBuilder().setImgHash(submission.getHash()).build();
    KissCaptchaImgMsg imgMsg = kissCaptchaImgStore.get(new ByteKey<>(key));

    ByteBuffer perceptualHash = imgMsg.getPerceptualHash();

    KissCaptchaBatchKeywordKey matchedKeywordKey =
        KissCaptchaBatchKeywordKey.newBuilder().setKeyword(submission.getKeyword())
            .setBatchId(submission.getBatchId()).build();
    KissCaptchaMatchedKeywordMsg matchedKeywordMsg = KissCaptchaMatchedKeywordMsg.newBuilder()
        .setKey(matchedKeywordKey)
        .setPerceptualHash(perceptualHash)
        .build();
    captchaMatchedKeywordMap.put(new ByteKey<>(matchedKeywordKey), matchedKeywordMsg);
    captchaKeywordMap.remove(new ByteKey<>(matchedKeywordKey));

//    log.info("Captcha submission: {}", submission);

    return "redirect:/captcha";
  }

  @GetMapping("/captcha/image/{hash}")
  public void showProductImage(@PathVariable("hash") String hash, HttpServletResponse response)
      throws IOException {
    response.setContentType("image/bmp");

    KissCaptchaImgMsg imgMsg = kissCaptchaImgStore
        .get(new ByteKey<>(KissCaptchaImgKey.newBuilder().setImgHash(hash).build()));

    ByteBuffer imgBytes = imgMsg.getImgBytes();
    InputStream is = new ByteArrayInputStream(imgBytes.array());
    ByteStreams.copy(is, response.getOutputStream());
  }

}
