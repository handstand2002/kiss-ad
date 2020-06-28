package com.brokencircuits.kissad.kissweb;

import com.brokencircuits.kissad.Extractor;
import com.gargoylesoftware.htmlunit.WebResponse;
import com.gargoylesoftware.htmlunit.html.DomNode;
import com.gargoylesoftware.htmlunit.html.DomNodeList;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CaptchaExtractors {

  public static Extractor<HtmlPage, Boolean> isCaptchaPageChecker(String findElement,
      String elementTextRegex) {
    Pattern elementRegexPattern = Pattern.compile(elementTextRegex);
    return page -> {
      DomNode domNode = page.getBody().querySelector(findElement);
      if (domNode != null) {
        Matcher matcher = elementRegexPattern.matcher(domNode.getTextContent());
        boolean isCaptchaPage = matcher.find();
        log.info("Page is captcha page?: {}; {}", isCaptchaPage, page.getBaseURL().toString());
        return isCaptchaPage;
      }
      return false;
    };
  }

  public static Extractor<HtmlPage, Collection<String>> keywordExtractor(String keywordSelector) {
    return page -> {
      DomNodeList<DomNode> keywordNodes = page.getBody().querySelectorAll(keywordSelector);

      List<String> keywords = new LinkedList<>();
      for (DomNode keywordNode : keywordNodes) {
        keywords.add(keywordNode.getFirstChild().asText());
      }
      keywords.forEach(word -> log.info("Keyword: {}", word));
      return keywords;
    };
  }

  public static Extractor<HtmlPage, Collection<BufferedImage>> imageExtractor(
      KissWebFetcher webFetcher, String imgSelector) {
    return page -> {
      DomNodeList<DomNode> imgNodes = page.getBody().querySelectorAll(imgSelector);

      List<BufferedImage> batchImages = new LinkedList<>();
      for (int i = 0; i < imgNodes.size(); i++) {
        DomNode imgNode = imgNodes.get(i);
        try {
          URL pageUrl = page.getBaseURL();
          String urlString = pageUrl.toString();
          String rootUrl = urlString.substring(0, urlString.indexOf(pageUrl.getPath()));

          String imgSrc = rootUrl + imgNode.getAttributes().getNamedItem("src").getTextContent();
          log.info("Image src [{}}]: {}", i, imgSrc);
          WebResponse imgResponse = webFetcher.fetchResource(imgSrc);
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

  private static BufferedImage resizeImage(BufferedImage originalImage, int newWidth,
      int newHeight) {
    BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, originalImage.getType());
    Graphics2D g = resizedImage.createGraphics();
    g.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
    g.dispose();

    return resizedImage;
  }
}
