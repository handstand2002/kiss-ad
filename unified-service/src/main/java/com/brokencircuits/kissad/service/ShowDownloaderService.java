package com.brokencircuits.kissad.service;

import com.brokencircuits.kissad.controller.FetcherService;
import com.brokencircuits.kissad.domain.CheckShowResult;
import com.brokencircuits.kissad.domain.ShowDto;
import com.brokencircuits.kissad.fetcher.SpFetcher;
import com.brokencircuits.kissad.repository.ShowRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
//@Service
@RequiredArgsConstructor
public class ShowDownloaderService {
  private final ShowRepository showRepository;
  private final TaskExecutor taskExecutor;
  private final FetcherService fetcherService;
  private final SpFetcher spFetcher;

  public void checkNewEpisodes(String showId) {
    Optional<ShowDto> show = showRepository.findById(showId);

    if (show.isPresent()) {
      taskExecutor.execute(() -> {
        CheckShowResult checkResult = checkShowInner(showId);
      });
    } else {
      log.error("Could not find show {}", showId);
    }
  }

  private CheckShowResult checkShowInner(String showUuid) {
    try {
      log.info("Checking for new episodes for show {}", showUuid);
      return fetcherService.fetch(showUuid);
    } catch (Exception e) {
      log.error("Could not checking for new episodes for {} due to ", showUuid, e);
      return new CheckShowResult(0, true);
    }
  }
}
