package com.brokencircuits.kissad.controller;

public interface WebSocketTopics {
  String SHOWS_TOPIC = "/topic/shows";
  String TOPIC_DL_STATUS = "/topic/downloader/status";
  String USER_QUEUE = "/queue/reply";

  static String episodeUpdates(String showId) {
    return String.format("/topic/shows/%s", showId);
  }
}
