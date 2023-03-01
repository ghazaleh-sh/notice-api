package ir.co.sadad.noticeapi.controllers;

import ir.co.sadad.noticeapi.dtos.*;
import ir.co.sadad.noticeapi.services.NoticeService;
import ir.co.sadad.noticeapi.services.ReactiveConsumerService;
import ir.co.sadad.noticeapi.services.ReactiveProducerService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
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

    //this is just for test
    @PostMapping(value = "/sample")
    public Mono<ResponseEntity<SendSingleNoticeResDto>> sendSingle(@RequestBody SendSingleNoticeReqDto singleNoticeReqDto) {
        return noticeService.sendSingleNotice(singleNoticeReqDto).map((res -> {

            return ResponseEntity.ok()
                    .body(res);
        }));
    }

    @PostMapping(value = "/campaign", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public Mono<ResponseEntity<SendCampaignNoticeResDto>> sendCampaign(@RequestPart("message") SendCampaignNoticeReqDto campaignNoticeReqDto,
                                                                       @RequestPart("file") FilePart filePart) {
        return noticeService.sendCampaignNotice(campaignNoticeReqDto, filePart).map((res -> ResponseEntity.ok().body(res)));
    }

    @GetMapping(value = "/notifications")
    public Mono<ResponseEntity<UserNoticeListResDto>> getNotifications(@RequestParam("ssn") String ssn,
                                                                       @RequestParam(value = "type", required = false) String type) {
        return noticeService.userNoticeList(ssn, type).map((res -> ResponseEntity.ok().body(res)));
    }
}
