package com.brokencircuits.kissad.domain.downloader.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
public class AriaProps {

  @Value("${download.aria.path}")
  private String ariaPath;
  @Value("${download.aria.port}")
  private int ariaRpcPort;
  @Value("${download.aria.temp-download-dir}")
  private String ariaTempDownloadDir;
  @Value("${download.aria.enabled}")
  private boolean enabled;
}
