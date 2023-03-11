package ir.co.sadad.noticeapi.services;

import ir.co.sadad.noticeapi.dtos.SendSingleNoticeReqDto;
import ir.co.sadad.noticeapi.dtos.SendSingleNoticeResDto;
import ir.co.sadad.noticeapi.models.Notification;
import ir.co.sadad.noticeapi.repositories.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.reactive.ReactiveKafkaConsumerTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

/**
 * a service as reactiveKafkaSource
 *
 * @author g.shahrokhabadi
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class ReactiveConsumerService {


    private final Scheduler scheduler = Schedulers.newSingle("sample", true);

    private final NotificationRepository notificationRepository;

    private final ReactiveKafkaConsumerTemplate<String, SendSingleNoticeReqDto> reactiveKafkaConsumerTemplate;

    /**
     * The code segment below consumes records from Kafka topics,
     * transforms the record and sends the output to an external sink.
     * Kafka consumer offsets are committed after records are successfully output to sink.
     */
    public Flux<String> consumeNotificationDTO() {
        return reactiveKafkaConsumerTemplate
                .receiveAutoAck()
//                .publishOn(scheduler)
                .concatMap(m -> storeInDB(m.value()))
//                     .thenEmpty(m.receiverOffset().commit()));
//                   or
//                      .doOnSuccess(r -> m.receiverOffset().commit().block()));
//                .receive()
//                // .delayElements(Duration.ofSeconds(2L)) // BACKPRESSURE
//                .doOnNext(consumerRecord -> log.info("received key={}, value={} from topic={}, offset={}",
//                        consumerRecord.key(),
//                        consumerRecord.value(),
//                        consumerRecord.topic(),
//                        consumerRecord.offset())
//                )
//                .map(ConsumerRecord::value)
//                .doOnNext(this::storeInDB)
                .doOnNext(data -> log.info("successfully consumed {}={}", SendSingleNoticeReqDto.class.getSimpleName(), data))
                .doOnError(throwable -> log.error("something bad happened while consuming : {}", throwable.getMessage()));
    }

    private Mono<String> storeInDB(SendSingleNoticeReqDto singleNoticeReqDto) {
        log.info("Successfully processed singleNoticeReqDto with title {} from Kafka", singleNoticeReqDto.getTitle());
        SendSingleNoticeResDto res = new SendSingleNoticeResDto();

        return Mono
                .just(singleNoticeReqDto)
                .flatMap(p -> notificationRepository.insert(Notification
                        .builder()
                        .title(p.getTitle())
                        .description(p.getDescription())
                        .date(p.getDate())
                        .type("1")
                        .build()))
//                .map(notif -> {
//                    res.setId(notif.getId());
//                    res.setSsn("000000");
//                    res.setStatus("success");
//                    return res;
//                });
                .doOnSuccess(result->System.out.print("Message saved in MongoDB with id: " + result.getId()))
                .map(Notification::getId);
//            return Mono.empty();
    }


}
