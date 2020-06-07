package com.brokenciruits.kissad.scheduler.config;

import com.brokencircuits.kissad.kafka.AdminInterface;
import com.brokencircuits.kissad.kafka.AnonymousConsumer;
import com.brokencircuits.kissad.kafka.ByteKey;
import com.brokencircuits.kissad.kafka.ClusterConnectionProps;
import com.brokencircuits.kissad.kafka.KeyValueStoreWrapper;
import com.brokencircuits.kissad.kafka.Publisher;
import com.brokencircuits.kissad.kafka.Topic;
import com.brokencircuits.kissad.messages.EpisodeMsg;
import com.brokencircuits.kissad.messages.EpisodeMsgKey;
import com.brokencircuits.kissad.messages.ShowMsg;
import com.brokencircuits.kissad.messages.ShowMsgKey;
import com.brokencircuits.kissad.runtime.AutostartService;
import com.brokencircuits.kissad.runtime.AutostartServiceWrapper;
import com.brokencircuits.kissad.topics.TopicUtil;
import com.brokencircuits.kissad.util.Uuid;
import com.brokencircuits.messages.Command;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.TopicPartitionInitialOffset.SeekPosition;

@Slf4j
@Configuration
public class KafkaConfig {

  public static final String STORE_SHOW = "show";
  private static final String STORE_EPISODE = "episode";

  @Bean
  KeyValueStoreWrapper<ByteKey<ShowMsgKey>, ShowMsg> showStoreWrapper(
      Topic<ByteKey<ShowMsgKey>, ShowMsg> showStoreTopic) {
    return new KeyValueStoreWrapper<>(STORE_SHOW, showStoreTopic);
  }

  @Bean
  Topic<ByteKey<ShowMsgKey>, ShowMsg> showStoreTopic(
      @Value("${messaging.schema-registry-url}") String schemaRegistryUrl) {
    return TopicUtil.showStoreTopic(schemaRegistryUrl);
  }

  @Bean
  Topic<ByteKey<EpisodeMsgKey>, EpisodeMsg> episodeStoreTopic(
      @Value("${messaging.schema-registry-url}") String schemaRegistryUrl) {
    return TopicUtil.episodeStoreTopic(schemaRegistryUrl);
  }

  @Bean
  KeyValueStoreWrapper<ByteKey<EpisodeMsgKey>, EpisodeMsg> episodeStoreWrapper(
      Topic<ByteKey<EpisodeMsgKey>, EpisodeMsg> episodeStoreTopic) {
    return new KeyValueStoreWrapper<>(STORE_EPISODE, episodeStoreTopic);
  }

  @Bean
  Topic<ByteKey<ShowMsgKey>, ShowMsg> showQueueTopic(
      @Value("${messaging.schema-registry-url}") String schemaRegistryUrl) {
    return TopicUtil.showQueueTopic(schemaRegistryUrl);
  }

  @Bean
  AnonymousConsumer<ByteKey<EpisodeMsgKey>, EpisodeMsg> episodeQueueConsumer(
      Topic<ByteKey<EpisodeMsgKey>, EpisodeMsg> episodeQueueTopic,
      ClusterConnectionProps connectionProps) {
    return new AnonymousConsumer<>(episodeQueueTopic, connectionProps, SeekPosition.END);
  }

  @Bean
  AutostartService autostartEpisodeQueueConsumer(
      AnonymousConsumer<ByteKey<EpisodeMsgKey>, EpisodeMsg> episodeQueueConsumer) {
    return new AutostartServiceWrapper(episodeQueueConsumer);
  }

  @Bean
  AutostartService autostartAdminInterface(AdminInterface adminInterface) {
    return new AutostartServiceWrapper(adminInterface);
  }

  @Bean
  Topic<ByteKey<EpisodeMsgKey>, EpisodeMsg> episodeQueueTopic(
      @Value("${messaging.schema-registry-url}") String schemaRegistryUrl) {
    return TopicUtil.episodeQueueTopic(schemaRegistryUrl);
  }

  @Bean
  public Function<Uuid, Boolean> triggerShowMethod(
      KeyValueStoreWrapper<ByteKey<ShowMsgKey>, ShowMsg> showStoreWrapper,
      Publisher<ByteKey<ShowMsgKey>, ShowMsg> showTriggerPublisher) {
    return showUuid -> {
      ShowMsgKey key = ShowMsgKey.newBuilder().setShowId(showUuid).build();
      ByteKey<ShowMsgKey> byteKey = ByteKey.from(key);
      ShowMsg showMsg = showStoreWrapper.get(byteKey);
      if (showMsg != null && showMsg.getValue() != null) {
        showTriggerPublisher.send(byteKey, showMsg);
        return true;
      }
      return false;
    };
  }


  @Bean
  AdminInterface adminInterface(@Value("${messaging.schema-registry-url}") String schemaRegistryUrl,
      ClusterConnectionProps props, Function<Uuid, Boolean> triggerShowMethod) throws Exception {

    AdminInterface adminInterface = new AdminInterface(props);
    adminInterface.registerCommand(Command.CHECK_NEW_EPISODES, command -> {
      Uuid showUuid = Uuid.fromString(command.getValue().getParameters().get(0));
      triggerShowMethod.apply(showUuid);
    });
    return adminInterface;
  }
}
