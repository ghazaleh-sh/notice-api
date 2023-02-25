package ir.co.sadad.noticeapi.configs;

import ir.co.sadad.noticeapi.dtos.SendSingleNoticeReqDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import reactor.kafka.sender.SenderOptions;

import java.util.Map;

/**
 * Creating a producer will write our messages to the topic.
 * It gets correctly properties from sever config
 *
 * TODO:
 * Along with buffer.memory and max.block.ms options on KafkaProducer,
 * maxInFlight enables control of memory and thread usage when KafkaSender is used in a reactive pipeline
 */
@Slf4j
@Configuration
public class ReactiveKafkaProducerConfig {

    @Bean
    public ReactiveKafkaProducerTemplate<String, SendSingleNoticeReqDto> reactiveKafkaProducerTemplate(
            KafkaProperties properties) {
        Map<String, Object> props = properties.buildProducerProperties();
        return new ReactiveKafkaProducerTemplate<>(SenderOptions.create(props));
    }

}
