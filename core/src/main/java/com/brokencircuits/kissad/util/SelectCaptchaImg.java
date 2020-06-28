package com.brokencircuits.kissad.util;

import com.gargoylesoftware.htmlunit.html.HtmlPage;
import java.io.IOException;

public interface SelectCaptchaImg {

  void selectImg(HtmlPage page, int imgIndex) throws IOException;
}
