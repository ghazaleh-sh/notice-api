package ir.co.sadad.noticeapi.configs;

import ir.co.sadad.noticeapi.dtos.SendSingleNoticeReqDto;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.IntegerSerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import org.springframework.stereotype.Service;
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

//    private static final String TOPIC = "noticeTopic";

    @Bean
    public ReactiveKafkaProducerTemplate<String, SendSingleNoticeReqDto> reactiveKafkaProducerTemplate(
            KafkaProperties properties) {
        Map<String, Object> props = properties.buildProducerProperties();
        return new ReactiveKafkaProducerTemplate<>(SenderOptions.create(props));
    }

}
