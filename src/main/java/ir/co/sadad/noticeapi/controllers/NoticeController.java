package ir.co.sadad.noticeapi.controllers;

import ir.co.sadad.noticeapi.dtos.SendCampaignNoticeReqDto;
import ir.co.sadad.noticeapi.dtos.SendCampaignNoticeResDto;
import ir.co.sadad.noticeapi.dtos.SendSingleNoticeReqDto;
import ir.co.sadad.noticeapi.dtos.SendSingleNoticeResDto;
import ir.co.sadad.noticeapi.services.NoticeService;
import ir.co.sadad.noticeapi.services.ReactiveConsumerService;
import ir.co.sadad.noticeapi.services.ReactiveProducerService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(path = "/kafka")
@RequiredArgsConstructor
public class NoticeController {

    private final ReactiveProducerService producerService;

    private final ReactiveConsumerService consumerService;

    private final NoticeService noticeService;


    @SneakyThrows
    @PostMapping(value = "/single-send", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<Void> sendSingleNoticeToKafka(@RequestBody SendSingleNoticeReqDto singleNoticeReqDto) throws InterruptedException {
        producerService.send(singleNoticeReqDto);
//        consumerService.run();
        return new ResponseEntity<>(HttpStatus.OK);
//                .map(r -> ResponseEntity.ok().<Void>build())
//                .defaultIfEmpty(ResponseEntity.notFound().build());
//    }

    }

    @GetMapping(value = "/consume")
    public ResponseEntity<Void> consumeData() {
        consumerService.consumeNotificationDTO().subscribe();
        return new ResponseEntity<>(HttpStatus.OK);
//                .map(r -> ResponseEntity.ok().<Void>build())
//                .defaultIfEmpty(ResponseEntity.notFound().build());
//    }

    }

    @PostMapping(value = "/sample")
    public Mono<ResponseEntity<SendSingleNoticeResDto>> sendSingle(@RequestBody SendSingleNoticeReqDto singleNoticeReqDto) {
        return noticeService.sendSingleNotice(singleNoticeReqDto).map((res -> {

            return ResponseEntity.ok()
                   .body(res);
        }));
    }

    @PostMapping(value = "/campaign")
    public Mono<ResponseEntity<SendCampaignNoticeResDto>> sendCampaign(@RequestBody SendCampaignNoticeReqDto campaignNoticeReqDto) {
        return noticeService.sendCampaignNotice(campaignNoticeReqDto).map((res ->  ResponseEntity.ok().body(res)));
    }
}
