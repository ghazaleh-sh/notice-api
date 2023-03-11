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
 * It gets correctly properties from server config
 *
 * @author g.shahrokhabadi
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
