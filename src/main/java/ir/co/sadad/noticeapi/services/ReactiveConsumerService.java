package ir.co.sadad.noticeapi.services;

import ir.co.sadad.noticeapi.dtos.SendSingleNoticeReqDto;
import ir.co.sadad.noticeapi.dtos.SendSingleNoticeResDto;
import ir.co.sadad.noticeapi.models.Notification;
import ir.co.sadad.noticeapi.models.UserNotification;
import ir.co.sadad.noticeapi.repositories.UserNotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.reactive.ReactiveKafkaConsumerTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;

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

    private final UserNotificationRepository userNotificationRepository;

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
        log.info("Successfully processed singleNoticeReqDto with title {} from Kafka", singleNoticeReqDto.getDate());
        SendSingleNoticeResDto res = new SendSingleNoticeResDto();

        List<Notification> notice = new ArrayList<>();
        notice.add(Notification.builder()
                .account(singleNoticeReqDto.getAccount())
                .balance(singleNoticeReqDto.getBalance())
                .withdraw(singleNoticeReqDto.getWithdraw())
                .date(singleNoticeReqDto.getDate())
                .bankName(singleNoticeReqDto.getBankName())
                .type("1")
                .build());

        return Mono
                .just(singleNoticeReqDto)
                .flatMap(p -> userNotificationRepository.insert(UserNotification
                        .builder()
                        .ssn(p.getSsn())
                        .notificationTransactions(notice)
//                        .lastSeenNotificationId()
//                        .remainNotificationCount()
//                        .notificationCount()
                        .build()))
//                .map(notif -> {
//                    res.setId(notif.getId());
//                    res.setSsn("000000");
//                    res.setStatus("success");
//                    return res;
//                });
                .doOnSuccess(result->System.out.print("Message saved in MongoDB with id: " + result.getId()))
                .map(UserNotification::getId);
//            return Mono.empty();
    }


}
