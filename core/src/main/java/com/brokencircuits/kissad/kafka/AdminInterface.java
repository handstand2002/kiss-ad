package com.brokencircuits.kissad.kafka;

import com.brokencircuits.kissad.topics.TopicUtil;
import com.brokencircuits.kissad.util.Service;
import com.brokencircuits.kissad.util.Uuid;
import com.brokencircuits.messages.AdminCommandKey;
import com.brokencircuits.messages.AdminCommandMsg;
import com.brokencircuits.messages.AdminCommandValue;
import com.brokencircuits.messages.Command;
import java.io.Closeable;
import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.StreamsConfig;
import org.springframework.kafka.support.TopicPartitionInitialOffset.SeekPosition;

@Slf4j
public class AdminInterface implements Service, Closeable {

  private final Topic<ByteKey<AdminCommandKey>, AdminCommandMsg> topic;
  private final String applicationId;
  private final AnonymousConsumer<ByteKey<AdminCommandKey>, AdminCommandMsg> consumer;
  private final Publisher<ByteKey<AdminCommandKey>, AdminCommandMsg> publisher;
  private final Map<Command, Consumer<AdminCommandMsg>> registeredCommands = new HashMap<>();

  public AdminInterface(String schemaRegistryUrl, ClusterConnectionProps connectionProps) {

    applicationId = connectionProps.asProperties().getProperty(StreamsConfig.APPLICATION_ID_CONFIG);
    if (applicationId == null) {
      throw new NullPointerException("property application.id cannot be null for AdminInterface");
    }

    topic = TopicUtil.adminTopic(schemaRegistryUrl);

    consumer = new AnonymousConsumer<>(topic, connectionProps, SeekPosition.END);
    consumer.setMessageListener(pair -> {
      if (pair.value().getKey().getApplicationId().equals(applicationId)) {
        log.info("Accepting command {}", pair.value());
        if (registeredCommands.containsKey(pair.value().getValue().getCommand())) {
          try {
            registeredCommands.get(pair.value().getValue().getCommand()).accept(pair.value());
          } catch (RuntimeException e) {
            log.error("Could not complete command {}", pair.value(), e);
          }
        } else {
          log.warn("Command {} has no registered action, ignoring",
              pair.value().getValue().getCommand());
        }
      }
    });

    publisher = new Publisher<>(connectionProps.asProperties(), topic);

    log.info("Configured AdminClient to receive commands for application.id {}", applicationId);
  }

  public void registerCommand(Command command, Consumer<AdminCommandMsg> consumer) {
    registeredCommands.put(command, consumer);
    log.info("Registered command {}", command);
  }

  public void sendCommand(String applicationId, Command command, String... parameters) {
    AdminCommandKey key = AdminCommandKey.newBuilder()
        .setApplicationId(applicationId)
        .setCommandId(Uuid.randomUUID())
        .build();
    AdminCommandValue value = AdminCommandValue.newBuilder()
        .setCommand(command)
        .setSendTime(Instant.now())
        .setParameters(Arrays.asList(parameters))
        .build();

    publisher
        .send(ByteKey.from(key), AdminCommandMsg.newBuilder().setKey(key).setValue(value).build());
  }

  @Override
  public void start() throws Exception {
    consumer.start();
  }

  @Override
  public void stop() {
    consumer.stop();
  }

  @Override
  public void close() throws IOException {
    stop();
  }
}
