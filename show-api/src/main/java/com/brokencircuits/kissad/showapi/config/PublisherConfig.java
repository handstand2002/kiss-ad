package com.brokencircuits.kissad.showapi.config;

import com.brokencircuits.kissad.kafka.ClusterConnectionProps;
import com.brokencircuits.kissad.kafka.Publisher;
import com.brokencircuits.kissad.kafka.Topic;
import com.brokencircuits.kissad.messages.ShowMsgKey;
import com.brokencircuits.kissad.messages.ShowMsgValue;
import com.brokencircuits.kissad.showapi.Intercept;
import java.util.Properties;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class PublisherConfig {

  @Bean
  Publisher<ShowMsgKey, ShowMsgValue> showMessagePublisher(
      Topic<ShowMsgKey, ShowMsgValue> showTopic, ClusterConnectionProps clusterProps) {
    Properties props = clusterProps.asProperties();
//    props.put(ProducerConfig.INTERCEPTOR_CLASSES_CONFIG, Intercept.class.getName());

    return new Publisher<>(props, showTopic);
  }

}
