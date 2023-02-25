package ir.co.sadad.noticeapi.configs;

import ir.co.sadad.noticeapi.dtos.SendSingleNoticeReqDto;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.reactive.ReactiveKafkaConsumerTemplate;
import reactor.kafka.receiver.ReceiverOptions;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
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

}
