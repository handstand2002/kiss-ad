package com.brokencircuits.kissad.restshow.poll;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.brokencircuits.kissad.kafka.KeyValueStore;
import com.brokencircuits.kissad.kafka.Publisher;
import com.brokencircuits.kissad.messages.KissShowMessage;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.junit.Before;
import org.junit.BeforeClass;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;

@Slf4j
public class PollNewEpisodeScheduleControllerTest {

  private KeyValueStore<Long, KissShowMessage> showMessageStore = mock(KeyValueStore.class);
  private ReadOnlyKeyValueStore<Long, KissShowMessage> innerStore = mock(
      ReadOnlyKeyValueStore.class);
  private KeyValueIterator<Long, KissShowMessage> innerStoreIterator = mock(KeyValueIterator.class);
  private Publisher<Long, KissShowMessage> showMessagePublisher = mock(Publisher.class);
  private static List<String> newEpisodePollSchedule = new ArrayList<>();
  private static TaskScheduler taskScheduler = new ConcurrentTaskScheduler();

  @BeforeClass
  public static void setupClass() {
    newEpisodePollSchedule.add("0 0 * * * *");
  }

  @Before
  public void setupTest() {
    when(showMessageStore.getStore()).thenReturn(innerStore);
    when(innerStore.all()).thenReturn(innerStoreIterator);
    when(innerStoreIterator.hasNext()).thenReturn(false);
  }

//  @Test
//  @SneakyThrows
//  public void test() {
//    PollNewEpisodeScheduleController controller = new PollNewEpisodeScheduleController(
//        taskScheduler, showMessageStore, showMessagePublisher, newEpisodePollSchedule);
//
//    controller.setDownloaderBusy(false);
//
//    log.info("Simulating a scheduled poll");
//    controller.trySendPoll();
//    controller.setDownloaderBusy(true);
//    log.info("");
//    Thread.sleep(1000);
//
//    log.info("Simulating a poll while the downloader is busy");
//    // This should cause a queue
//    controller.trySendPoll();
//    log.info("");
//
//    log.info("Simulate the downloader finishing the previous task");
//    controller.setDownloaderBusy(false);
//    log.info("");
//
//    Thread.sleep(5000);
//    controller.setDownloaderBusy(true);
//
//
//    Thread.sleep(60000);
//  }

}