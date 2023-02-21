package ir.co.sadad.noticeapi.configs;

import ir.co.sadad.noticeapi.dtos.SendSingleNoticeReqDto;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.reactive.ReactiveKafkaConsumerTemplate;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.stereotype.Service;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.kafka.receiver.ReceiverOptions;

import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Consumer is  the service that will be responsible for reading messages processing them according to
 * the needs of your own business logic.
 */
@Slf4j
@Configuration
public class ReactiveKafkaConsumerConfig {

    private static final String TOPIC = "noticeTopic";

    @Bean
    public ReceiverOptions<String, SendSingleNoticeReqDto> kafkaReceiverOptions(KafkaProperties kafkaProperties) {

        final Map<String, Object> map = new HashMap<>(kafkaProperties.buildConsumerProperties());
        map.put(ConsumerConfig.GROUP_ID_CONFIG, "MyGroup");
        map.put(ConsumerConfig.RECONNECT_BACKOFF_MS_CONFIG, 10_000L);

        ReceiverOptions<String, SendSingleNoticeReqDto> basicReceiverOptions = ReceiverOptions.create(map);
        return basicReceiverOptions
                .commitInterval(Duration.ZERO)
                .commitBatchSize(0)
                .subscription(Collections.singletonList(TOPIC));
    }

    @Bean
    public ReactiveKafkaConsumerTemplate<String, SendSingleNoticeReqDto> reactiveKafkaConsumerTemplate(ReceiverOptions<String, SendSingleNoticeReqDto> kafkaReceiverOptions) {
        return new ReactiveKafkaConsumerTemplate<>(kafkaReceiverOptions);
    }

//    @Bean
//    public Scheduler messageFlowScheduler() {
//        return Schedulers.newParallel("message-flow-scheduler", customKafkaProperties.getFlowSchedulerConcurrency());
//    }
//
//    @Bean
//    public List<ReactiveKafkaConsumerTemplate<String, RawMessage>> kafkaConsumerTemplates(Scheduler messageFlowScheduler) {
//        final String consumerConcurrency = customKafkaProperties.getReactiveConsumer().getConsumerConcurrency();
//        log.info("Kafka consumer concurrency level is {}", (Object) consumerConcurrency);
//        final List<ReactiveKafkaConsumerTemplate<String, RawMessage>> consumers = new ArrayList<ReactiveKafkaConsumerTemplate<String, RawMessage>>();
//        for (int i = 0; i < consumerConcurrency; i++) {
//            final var kafkaReceiverOptions = ReceiverOptions.<String, RawMessage>create(kafkaProperties.buildConsumerProperties())
//                    .schedulerSupplier(() -> messageFlowScheduler)
//                    .subscription(singleton(topicsProperties.getGlobalRawTopic()));
//            consumers.add(new ReactiveKafkaConsumerTemplate<>(kafkaReceiverOptions));
//        }
//        return consumers;
//    }
}
