package com.brokencircuits.kissad.kissweb;

import com.brokencircuits.kissad.Extractor;
import com.brokencircuits.kissad.util.SelectCaptchaImg;
import com.brokencircuits.kissad.util.SubmitForm;
import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import java.awt.image.BufferedImage;
import java.util.Collection;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class CaptchaAutoconfig {

  @Bean
  Extractor<HtmlPage, Boolean> isCaptchaPageChecker(
      @Value("${kiss.captcha.page-identifier.element-selector: #container>.bigBarContainer>.barTitle}") String findElementSelector,
      @Value("${kiss.captcha.page-identifier.regex-match: Are you human}") String elementTextRegex) {
    return CaptchaExtractors.isCaptchaPageChecker(findElementSelector, elementTextRegex);
  }

  @Bean
  Extractor<HtmlPage, Collection<BufferedImage>> imageExtractor(KissWebFetcher webFetcher,
      @Value("${kiss.captcha.all-img-css-selector: #container #formVerify1 img}") String imgSelector) {
    return CaptchaExtractors.imageExtractor(webFetcher, imgSelector);
  }

  @Bean
  Extractor<HtmlPage, Collection<String>> keywordExtractor(
      @Value("${kiss.captcha.all-keyword-css-selector: #container #formVerify1>div>p>span}") String keywordSelector) {
    return CaptchaExtractors.keywordExtractor(keywordSelector);
  }

  @Bean
  SelectCaptchaImg selectCaptchaImg(
      @Value("${kiss.captcha.form-selector: #formVerify1}") String formSelector) {
    return (page, imgIndex) -> {

      HtmlInput answerCap = ((HtmlForm) page.getBody().querySelector(formSelector))
          .getInputByName("answerCap");

      answerCap.setValueAttribute(answerCap.getValueAttribute() + imgIndex + ",");
    };
  }

  @Bean
  SubmitForm submitForm(@Value("${kiss.captcha.form-selector: #formVerify1}") String formSelector) {
    return page -> {
      // create a submit button - it doesn't work with 'input'
      DomElement button = page.createElement("button");
      button.setAttribute("type", "submit");

      HtmlForm form = page.getBody().querySelector("#formVerify1");
      // append the button to the form
      if (form == null) {
        log.info("Not on a captcha page, apparently - couldn't find verify form");
        return page;
      }
      form.appendChild(button);

      // submit the form
      return button.click();
    };
  }
}
