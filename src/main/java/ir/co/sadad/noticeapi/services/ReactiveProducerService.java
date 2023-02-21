package ir.co.sadad.noticeapi.services;

import ir.co.sadad.noticeapi.dtos.SendSingleNoticeReqDto;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ReactiveProducerService {

    private final ReactiveKafkaProducerTemplate<String, SendSingleNoticeReqDto> reactiveKafkaProducerTemplate;
    private final ReactiveConsumerService consumerService;

    @Value(value = "${spring.kafka.producer.topic}")
    private String topic;

    public ReactiveProducerService(ReactiveKafkaProducerTemplate<String, SendSingleNoticeReqDto> reactiveKafkaProducerTemplate, ReactiveConsumerService consumerService) {
        this.reactiveKafkaProducerTemplate = reactiveKafkaProducerTemplate;
        this.consumerService = consumerService;
    }

    //Subscribe to trigger the actual flow of records from outbound message to Kafka.
    public void send(SendSingleNoticeReqDto singleNoticeReqDto) throws InterruptedException {
        log.info("send to topic={}, {}={},", topic, SendSingleNoticeReqDto.class.getSimpleName(), singleNoticeReqDto);
        reactiveKafkaProducerTemplate.send(topic, singleNoticeReqDto)
                .doOnError(e -> log.error("Send failed", e))
//                .doOnSuccess(senderResult -> log.info("sent {} offset : {}", fakeProducerDTO, senderResult.recordMetadata().offset()))
//                .subscribe();
                .doOnSuccess((r) -> consumerService.consumeNotificationDTO().subscribe())
                .subscribe(senderResult -> {
                    log.info("sent {} offset : {}", singleNoticeReqDto, senderResult.recordMetadata().offset());
                    RecordMetadata metadata = senderResult.recordMetadata();
                    System.out.printf("Message sent successfully, topic-partition=%s-%d offset=%d\n",
                            metadata.topic(),
                            metadata.partition(),
                            metadata.offset());
                });
    }
}
