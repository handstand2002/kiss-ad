package com.brokencircuits.kissad.controller;


import com.brokencircuits.kissad.domain.ShowDto;
import com.brokencircuits.kissad.fetcher.SpFetcher;
import com.brokencircuits.kissad.repository.ShowRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class FetcherController {

  private final SpFetcher spFetcher;
  private final ShowRepository showRepository;

  public void fetch(UUID showUuid) {

    Optional<ShowDto> show = showRepository.findById(showUuid.toString());
    if (!show.isPresent()) {
      log.error("Show doesn't exist for Uuid: {}", showUuid);
      return;
    }
    ShowDto showDto = show.get();
    if (showDto.getSourceName().equals(SpFetcher.SOURCE_IDENTIFIER)) {
      spFetcher.process(showDto);
    } else {
      log.error("Received message for fetcher: {}", showDto.getSourceName());
    }
  }
}
