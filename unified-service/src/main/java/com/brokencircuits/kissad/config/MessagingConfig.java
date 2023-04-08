package com.brokencircuits.kissad.config;

import com.brokencircuits.download.messages.DownloadType;
import com.brokencircuits.kissad.domain.DownloadTypeDto;
import com.brokencircuits.kissad.domain.EpisodeDto;
import com.brokencircuits.kissad.domain.EpisodeId;
import com.brokencircuits.kissad.domain.EpisodeLinkDto;
import com.brokencircuits.kissad.domain.ShowDto;
import com.brokencircuits.kissad.fetcher.SpFetcher;
import com.brokencircuits.kissad.messages.EpisodeLink;
import com.brokencircuits.kissad.messages.EpisodeMsg;
import com.brokencircuits.kissad.messages.EpisodeMsgKey;
import com.brokencircuits.kissad.messages.EpisodeMsgValue;
import com.brokencircuits.kissad.messages.ShowMsg;
import com.brokencircuits.kissad.messages.ShowMsgKey;
import com.brokencircuits.kissad.messages.ShowMsgValue;
import com.brokencircuits.kissad.repository.EpisodeRepository;
import com.brokencircuits.kissad.repository.RepositoryBasedTable;
import com.brokencircuits.kissad.repository.ShowRepository;
import com.brokencircuits.kissad.table.ReadWriteTable;
import com.brokencircuits.kissad.util.KeyValue;
import com.brokencircuits.kissad.util.Uuid;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
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

  @Bean
  ReadWriteTable<EpisodeMsgKey, EpisodeMsg> episodeTable(
      EpisodeRepository episodeRepository) {

    Function<KeyValue<EpisodeMsgKey, EpisodeMsg>, EpisodeDto> convertToEntity = kv -> convertEpisode(
        kv.getValue());
    Function<EpisodeDto, KeyValue<EpisodeMsgKey, EpisodeMsg>> convertToKeyValue = this::convertEpisode;
    Function<EpisodeMsgKey, EpisodeId> convertKey = b -> new EpisodeId(
        b.getShowId().getShowId().toString(), Math.toIntExact(b.getEpisodeNumber()));

    return new RepositoryBasedTable<>(episodeRepository, convertToEntity, convertToKeyValue,
        convertKey);
  }

  private KeyValue<EpisodeMsgKey, EpisodeMsg> convertEpisode(EpisodeDto dto) {
    EpisodeMsgKey key = EpisodeMsgKey.newBuilder()
        .setShowId(ShowMsgKey.newBuilder().setShowId(Uuid.fromString(
            dto.getShowId())).build()).setEpisodeNumber((long) dto.getEpisodeNumber())
        .setRawTitle(null).build();

    EpisodeMsg value = EpisodeMsg.newBuilder()
        .setKey(key)
        .setValue(EpisodeMsgValue.newBuilder()
            .setDownloadTime(dto.getDownloadTime())
            .setDownloadedQuality(dto.getDownloadedQuality())
            .setLatestLinks(convertLinksToMsg(dto.getLatestLinks()))
            .setMessageId(Uuid.randomUUID())
            .build())
        .build();
    return KeyValue.of(key, value);
  }

  private List<EpisodeLink> convertLinksToMsg(List<EpisodeLinkDto> latestLinks) {
    List<EpisodeLink> output = new ArrayList<>(latestLinks.size());
    for (EpisodeLinkDto link : latestLinks) {
      output.add(EpisodeLink.newBuilder()
          .setQuality(link.getQuality())
          .setType(DownloadType.valueOf(link.getType().name()))
          .setUrl(link.getUrl())
          .build());
    }
    return output;
  }

  private EpisodeDto convertEpisode(EpisodeMsg value) {
    return EpisodeDto.builder()
        .showId(value.getKey().getShowId().getShowId().toString())
        .episodeNumber(Math.toIntExact(value.getKey().getEpisodeNumber()))
        .downloadTime(value.getValue().getDownloadTime())
        .downloadedQuality(value.getValue().getDownloadedQuality())
        .latestLinks(convertLinksToDto(value.getValue().getLatestLinks()))
        .build();
  }

  private List<EpisodeLinkDto> convertLinksToDto(List<EpisodeLink> latestLinks) {
    List<EpisodeLinkDto> output = new LinkedList<>();
    for (EpisodeLink link : latestLinks) {
      output.add(
          new EpisodeLinkDto(link.getQuality(), DownloadTypeDto.valueOf(link.getType().name()),
              link.getUrl()));
    }
    return output;
  }

  private ShowDto convertShow(ShowMsg msg) {
    String url = msg.getValue().getSources().get(SpFetcher.SOURCE_IDENTIFIER);
    if (url == null) {
      throw new IllegalStateException(
          "Could not find correct source for show. Values in source map: " + msg.getValue()
              .getSources());
    }
    return ShowDto.builder()
        .id(msg.getKey().getShowId().toString())
        .title(msg.getValue().getTitle())
        .isActive(msg.getValue().getIsActive())
        .season(msg.getValue().getSeason())
        .releaseScheduleCron(msg.getValue().getReleaseScheduleCron())
        .skipEpisodeString(msg.getValue().getSkipEpisodeString())
        .episodeNamePattern(msg.getValue().getEpisodeNamePattern())
        .folderName(msg.getValue().getFolderName())
        .sourceType(SpFetcher.SOURCE_IDENTIFIER)
        .source(url)
        .build();
  }

  @Bean
  ReadWriteTable<ShowMsgKey, ShowMsg> showTable(ShowRepository showRepository) {

    Function<KeyValue<ShowMsgKey, ShowMsg>, ShowDto> convertToEntity = kv -> convertShow(
        kv.getValue());
    Function<ShowDto, KeyValue<ShowMsgKey, ShowMsg>> convertToKv = dto -> {
      ShowMsg showMsg = convertShow(dto);
      return KeyValue.of(showMsg.getKey(), showMsg);
    };
    Function<ShowMsgKey, String> convertKey = k -> k.getShowId().toString();

    return new RepositoryBasedTable<>(showRepository, convertToEntity, convertToKv, convertKey);
  }

  private ShowMsg convertShow(ShowDto dto) {
    ShowMsgKey key = ShowMsgKey.newBuilder()
        .setShowId(Uuid.fromString(dto.getId()))
        .build();
    Map<String, String> sources = new HashMap<>();
    sources.put(dto.getSourceType(), dto.getSource());
    ShowMsgValue value = ShowMsgValue.newBuilder()
        .setMessageId(Uuid.randomUUID())
        .setTitle(dto.getTitle())
        .setSeason(dto.getSeason())
        .setSources(sources)
        .setIsActive(Optional.ofNullable(dto.getIsActive()).orElse(true))
        .setReleaseScheduleCron(dto.getReleaseScheduleCron())
        .setSkipEpisodeString(dto.getSkipEpisodeString())
        .setEpisodeNamePattern(dto.getEpisodeNamePattern())
        .setFolderName(dto.getFolderName())
        .build();
    return ShowMsg.newBuilder()
        .setKey(key)
        .setValue(value)
        .build();
  }

}
