package com.brokencircuits.kissad.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.CommonsRequestLoggingFilter;
import org.springframework.web.servlet.FrameworkServlet;

@Slf4j
@Configuration
public class MessagingConfig {

  @Bean
  public Object frameworkServlet(FrameworkServlet servlet) {
    servlet.setEnableLoggingRequestDetails(true);
    return new Object();
  }

  @Bean
  public CommonsRequestLoggingFilter requestLoggingFilter() {
    CommonsRequestLoggingFilter requestLoggingFilter = new CommonsRequestLoggingFilter();
    requestLoggingFilter.setIncludeClientInfo(true);
    requestLoggingFilter.setIncludeHeaders(true);
    requestLoggingFilter.setIncludeQueryString(true);
    requestLoggingFilter.setIncludePayload(true);
    return requestLoggingFilter;
  }

}
