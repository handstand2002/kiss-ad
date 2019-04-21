package com.brokencircuits.kissad;

import com.gargoylesoftware.htmlunit.html.HtmlPage;
import java.io.IOException;

public interface WebFetcher {

  HtmlPage fetchPage(String url) throws IOException;
}
