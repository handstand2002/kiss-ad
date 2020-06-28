package com.brokencircuits.kissad.novaepisodefetch.streams;

import com.brokencircuits.download.messages.DownloadType;
import com.brokencircuits.kissad.kafka.ByteKey;
import com.brokencircuits.kissad.kafka.Publisher;
import com.brokencircuits.kissad.messages.EpisodeLink;
import com.brokencircuits.kissad.messages.EpisodeMsg;
import com.brokencircuits.kissad.messages.EpisodeMsgKey;
import com.brokencircuits.kissad.messages.EpisodeMsgValue;
import com.brokencircuits.kissad.messages.KissEpisodeExternalSrcMsg;
import com.brokencircuits.kissad.novaepisodefetch.domain.NovaShowDetails;
import com.brokencircuits.kissad.novaepisodefetch.nova.NovaApiClient;
import com.brokencircuits.kissad.util.Uuid;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.processor.Processor;
import org.apache.kafka.streams.processor.ProcessorContext;

@Slf4j
@RequiredArgsConstructor
public class EpisodeProcessor implements
    Processor<ByteKey<EpisodeMsgKey>, KissEpisodeExternalSrcMsg> {

  private final NovaApiClient novaApiClient;
  private final Publisher<ByteKey<EpisodeMsgKey>, EpisodeMsg> episodeMsgPublisher;

  private static final Pattern SHOW_ID_REGEX = Pattern.compile("([^/]+)$");
  private static final Pattern RESOLUTION_REGEX = Pattern.compile("(\\d+)[pP]");

  @Override
  public void init(ProcessorContext context) {

  }

  @Override
  public void process(ByteKey<EpisodeMsgKey> key, KissEpisodeExternalSrcMsg value) {
    try {

      Matcher showIdMatcher = SHOW_ID_REGEX.matcher(value.getUrl());
      if (showIdMatcher.find()) {
        String showId = showIdMatcher.group(1);
        NovaShowDetails novaShowDetails = novaApiClient.create(showId);
        log.info("API Result: {}", novaShowDetails);
        HttpURLConnection.setFollowRedirects(false);

        if (novaShowDetails.getData().isEmpty()) {
          log.warn("Could not fetch show details from API");
        } else {
          List<EpisodeLink> latestLinks = novaShowDetails.getData().stream()
              .map(linkDetails -> EpisodeLink.newBuilder()
                  .setQuality(getQualityFromLabel(linkDetails.getLabel()))
                  .setUrl(getRedirectUrl(linkDetails.getFile()).orElse(linkDetails.getFile()))
                  .setType(DownloadType.DIRECT)
                  .build())
              .collect(Collectors.toList());

          EpisodeMsg foundEpisodeMsg = EpisodeMsg.newBuilder()
              .setKey(value.getEpisodeMsgKey())
              .setValue(EpisodeMsgValue.newBuilder()
                  .setDownloadedQuality(0)
                  .setDownloadTime(null)
                  .setMessageId(Uuid.randomUUID())
                  .setLatestLinks(latestLinks)
                  .build())
              .build();

          log.info("Retrieved episode info: {}", foundEpisodeMsg);
          episodeMsgPublisher.send(key, foundEpisodeMsg);
        }
      }

    } catch (Exception e) {
      log.error("Error processing: ", e);
    }

  }

  private int getQualityFromLabel(String label) {
    Matcher matcher = RESOLUTION_REGEX.matcher(label);
    if (matcher.find()) {
      return Integer.parseInt(matcher.group(1));
    }
    return 0;
  }

  private Optional<String> getRedirectUrl(String redirectUrl) {
    try {
      HttpURLConnection connection = (HttpURLConnection) new URL(redirectUrl).openConnection();
      return Optional.ofNullable(connection.getHeaderField("Location"));
    } catch (IOException e) {
      log.error("Was not able to get open connection with redirect URL: {}", redirectUrl, e);
    }
    return Optional.empty();
  }

  @Override
  public void close() {

  }
}
