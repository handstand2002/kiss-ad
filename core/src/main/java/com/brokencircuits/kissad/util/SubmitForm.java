package com.brokencircuits.kissad.util;

import com.gargoylesoftware.htmlunit.html.HtmlPage;
import java.io.IOException;

public interface SubmitForm {

  HtmlPage submit(HtmlPage page) throws IOException;
}
