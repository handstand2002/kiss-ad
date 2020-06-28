package com.brokencircuits.kissad.kissepisodefetch.streams;

import com.brokencircuits.kissad.Extractor;
import com.brokencircuits.kissad.Translator;
import com.brokencircuits.kissad.kafka.ByteKey;
import com.brokencircuits.kissad.kafka.KeyValueStoreWrapper;
import com.brokencircuits.kissad.kafka.Publisher;
import com.brokencircuits.kissad.kissweb.KissWebFetcher;
import com.brokencircuits.kissad.messages.EpisodeMsgKey;
import com.brokencircuits.kissad.messages.ExternalSources;
import com.brokencircuits.kissad.messages.KissEpisodeExternalSrcMsg;
import com.brokencircuits.kissad.messages.KissEpisodePageKey;
import com.brokencircuits.kissad.messages.KissEpisodePageMessage;
import com.brokencircuits.kissad.util.SelectCaptchaImg;
import com.brokencircuits.kissad.util.SubmitForm;
import com.brokencircuits.kissad.util.Uuid;
import com.brokencircuits.messages.KissCaptchaBatchKeywordKey;
import com.brokencircuits.messages.KissCaptchaBatchKeywordMsg;
import com.brokencircuits.messages.KissCaptchaImgKey;
import com.brokencircuits.messages.KissCaptchaImgMsg;
import com.brokencircuits.messages.KissCaptchaMatchedKeywordMsg;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.github.kilianB.hash.Hash;
import com.github.kilianB.hashAlgorithms.HashingAlgorithm;
import com.github.kilianB.hashAlgorithms.PerceptiveHash;
import java.awt.image.BufferedImage;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.processor.Processor;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.state.KeyValueIterator;

@Slf4j
@RequiredArgsConstructor
public class EpisodeProcessor implements
    Processor<ByteKey<KissEpisodePageKey>, KissEpisodePageMessage> {

  private final KissWebFetcher webClient;
  private final Extractor<HtmlPage, Collection<BufferedImage>> imageExtractor;
  private final Extractor<HtmlPage, Collection<String>> keywordExtractor;
  private final KeyValueStoreWrapper<ByteKey<KissCaptchaBatchKeywordKey>, KissCaptchaMatchedKeywordMsg> matchedKeywordsStore;
  private final SelectCaptchaImg selectCaptchaImg;
  private final SubmitForm submitForm;
  private final Extractor<HtmlPage, Boolean> isCaptchaPageChecker;
  private final Extractor<HtmlPage, String> episodeIframeExtractor;
  private final Publisher<ByteKey<EpisodeMsgKey>, KissEpisodeExternalSrcMsg> externalSrcMsgPublisher;

  private final Publisher<ByteKey<KissCaptchaImgKey>, KissCaptchaImgMsg> kissCaptchaPicturePublisher;
  private final Publisher<ByteKey<KissCaptchaBatchKeywordKey>, KissCaptchaBatchKeywordMsg> kissCaptchaKeywordPublisher;
  private final Translator<BufferedImage, KeyValue<ByteKey<KissCaptchaImgKey>, KissCaptchaImgMsg>> imgToMsgTranslator;


  private final static HashingAlgorithm perceptualHash = new PerceptiveHash(32);

  private static Hash reconstructHash(ByteBuffer buffer) {
    return new Hash(new BigInteger(buffer.array()),
        perceptualHash.getKeyResolution(), perceptualHash.algorithmId());
  }

  @Override
  public void init(ProcessorContext context) {
  }

  @Override
  public void process(ByteKey<KissEpisodePageKey> key, KissEpisodePageMessage msg) {
    log.info("Processing show: {} | {}", key, msg);

    try {

      // TODO: handle more external sources
      String url = setUrlServer(msg.getUrl(), "nova");
      log.info("Navigating to {}", url);

      HtmlPage page = solveCaptchaAndReload(url);

      String iframeUrl = episodeIframeExtractor.extract(page);

      EpisodeMsgKey episodeMsgKey = EpisodeMsgKey.newBuilder()
          .setRawTitle(msg.getEpisodeName())
          .setEpisodeNumber(Long.valueOf(msg.getEpisodeNumber()))
          .setShowId(msg.getKey().getShowKey())
          .build();
      KissEpisodeExternalSrcMsg externalMsg = KissEpisodeExternalSrcMsg.newBuilder()
          .setEpisodeMsgKey(episodeMsgKey)
          .setSource(ExternalSources.NOVA)
          .setUrl(iframeUrl)
          .build();
      externalSrcMsgPublisher.send(new ByteKey<>(episodeMsgKey), externalMsg);

    } catch (Exception e) {
      log.error("Unable to process {} due to Exception", msg, e);
    }
  }

  private String setUrlServer(String url, String server) {
    Pattern p = Pattern.compile("\\?[^=]+=");
    url = url.replaceAll("[&?]s=[^&]*", "");

    Matcher containsGetVariables = p.matcher(url);
    if (containsGetVariables.find()) {
      url = url + "&s=" + server;
    } else {
      url = url + "?s=" + server;
    }
    return url;
  }

  private HtmlPage solveCaptchaAndReload(String url)
      throws Exception {
    Collection<Integer> captchaImgIndexes;
    Collection<String> keywords;
    List<BufferedImage> images;
    AtomicReference<HtmlPage> page = new AtomicReference<>(null);
    boolean isCaptchaPage;
    do {
      do {
        log.info("Fetching captcha page {}", url);
        page.set(webClient.fetchPage(url));

        log.info("Extracting images and keywords from page");
        images = (List<BufferedImage>) imageExtractor.extract(page.get());
        keywords = keywordExtractor.extract(page.get());

        List<Hash> pageImgHashes = images.stream().map(perceptualHash::hash)
            .collect(Collectors.toList());

        captchaImgIndexes = solveCaptcha(keywords, pageImgHashes);
        Thread.sleep(5000);
        if (captchaImgIndexes.size() < keywords.size()) {
          publishUnsolvedCaptcha(images, keywords);
        }
      } while (captchaImgIndexes.size() < keywords.size());

      for (Integer imgIndex : captchaImgIndexes) {
        log.info("Selecting img {} as solution to captcha", imgIndex);
        selectCaptchaImg.selectImg(page.get(), imgIndex);
      }
      log.info("Submitting captcha");
      submitForm.submit(page.get());
      Thread.sleep(5000);

      log.info("Reloading page after submitting captcha");
      page.set(webClient.fetchPage(url));
      isCaptchaPage = isCaptchaPageChecker.extract(page.get());
      log.info("Captcha successfully solved: {}", !isCaptchaPage);
      if (isCaptchaPage) {
        publishUnsolvedCaptcha(images, keywords);
      }
    } while (isCaptchaPage);
    return page.get();
  }

  private void publishUnsolvedCaptcha(Collection<BufferedImage> images,
      Collection<String> keywords) {

    if (images.size() > 8) {
      log.info("Not publishing captcha since it contains too many images");
      return;
    }
    log.info("Publishing captcha that contains unknown keywords or images");
    Set<KeyValue<ByteKey<KissCaptchaImgKey>, KissCaptchaImgMsg>> messages = new HashSet<>();
    images.forEach(img -> messages.add(imgToMsgTranslator.translate(img)));

    Uuid batchId = Uuid.randomUUID();

    keywords.forEach(phrase -> {
      KissCaptchaBatchKeywordKey key = KissCaptchaBatchKeywordKey.newBuilder()
          .setBatchId(batchId).setKeyword(phrase).build();
      ByteKey<KissCaptchaBatchKeywordKey> byteKey = new ByteKey<>(key);
      KissCaptchaBatchKeywordMsg keywordMsg = KissCaptchaBatchKeywordMsg.newBuilder()
          .setImageKeys(imageKeys(messages)).setKey(key).build();
      kissCaptchaKeywordPublisher.send(byteKey, keywordMsg);
    });

    messages.forEach(kissCaptchaPicturePublisher::send);
  }

  private List<KissCaptchaImgKey> imageKeys(
      Set<KeyValue<ByteKey<KissCaptchaImgKey>, KissCaptchaImgMsg>> msgList) {
    return msgList.stream().map(kvPair -> kvPair.value.getKey()).collect(Collectors.toList());
  }

  private Collection<Integer> solveCaptcha(Collection<String> keywords, List<Hash> pageImgHashes) {
    Collection<Integer> correctPageImages = new LinkedList<>();

    for (String keyword : keywords) {
      int bestImgIndex = -1;
      int bestImgDistance = Integer.MAX_VALUE;
      List<Hash> hashesForWord = getMatchedPerceptualHashesForWord(keyword)
          .stream().map(EpisodeProcessor::reconstructHash)
          .collect(Collectors.toList());

      for (Hash storedGoodHash : hashesForWord) {
        for (int i = 0; i < pageImgHashes.size(); i++) {
          Hash pageImgHash = pageImgHashes.get(i);
          int hashDistance = pageImgHash.hammingDistance(storedGoodHash);
          if (hashDistance < bestImgDistance) {
            bestImgDistance = hashDistance;
            bestImgIndex = i;
          }
        }
      }

      if (bestImgIndex < 0) {
        log.info("Keyword {} does not have any saved hash", keyword);
      } else {
        correctPageImages.add(bestImgIndex);
        log.info("Best match for keyword {} is img {}", keyword, bestImgIndex);
      }
    }
    return correctPageImages;
  }

  private Collection<ByteBuffer> getMatchedPerceptualHashesForWord(String keyword) {
    KeyValue<ByteKey<KissCaptchaBatchKeywordKey>, ByteKey<KissCaptchaBatchKeywordKey>> range = ByteKey
        .rangeFrom(keyword);
    Collection<ByteBuffer> hashes = new LinkedList<>();
    try (KeyValueIterator<ByteKey<KissCaptchaBatchKeywordKey>, KissCaptchaMatchedKeywordMsg> iterator = matchedKeywordsStore
        .range(range.key, range.value)) {
      while (iterator.hasNext()) {
        KeyValue<ByteKey<KissCaptchaBatchKeywordKey>, KissCaptchaMatchedKeywordMsg> entry = iterator
            .next();
        hashes.add(entry.value.getPerceptualHash());
      }
    }

    return hashes;
  }

  @Override
  public void close() {

  }
}
