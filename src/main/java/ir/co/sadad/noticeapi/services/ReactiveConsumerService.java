package ir.co.sadad.noticeapi.services;

import ir.co.sadad.noticeapi.dtos.SendSingleNoticeReqDto;
import ir.co.sadad.noticeapi.dtos.SendSingleNoticeResDto;
import ir.co.sadad.noticeapi.models.Notification;
import ir.co.sadad.noticeapi.repositories.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.reactive.ReactiveKafkaConsumerTemplate;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.*;


@Slf4j
@RequiredArgsConstructor
@Service
public class ReactiveConsumerService {//implements CommandLineRunner {


    private final Scheduler scheduler = Schedulers.newSingle("sample", true);

    private final NotificationRepository notificationRepository;

    private final ReactiveKafkaConsumerTemplate<String, SendSingleNoticeReqDto> reactiveKafkaConsumerTemplate;

    List<Disposable> disposables = new ArrayList<>();

//    public void flux() {
//        reactiveKafkaConsumerTemplate.receive()
//                .publishOn(scheduler)
//                .concatMap(m -> storeInDB(m.value())
//                        .thenEmpty(m.receiverOffset().commit()));
////                .retry()
////                .doOnCancel(this::close);
//    }

    public Mono<String> storeInDB(SendSingleNoticeReqDto singleNoticeReqDto) {
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

    public void close() {
        for (Disposable disposable : disposables)
            disposable.dispose();
        scheduler.dispose();
    }

//    public void runScenario() throws InterruptedException {
//        flux();
//        close();
//    }

    public Flux<String> consumeNotificationDTO() {
        return reactiveKafkaConsumerTemplate
                .receiveAutoAck()
//                .publishOn(scheduler)
                .concatMap(m -> storeInDB(m.value()))
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

//    @Override
//    public void run(String... args) throws Exception {
//        // we have to trigger consumption
//        consumeNotificationDTO().subscribe();
//    }

}
