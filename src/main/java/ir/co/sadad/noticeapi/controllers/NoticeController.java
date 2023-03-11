package ir.co.sadad.noticeapi.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import ir.co.sadad.noticeapi.dtos.*;
import ir.co.sadad.noticeapi.models.UserNotification;
import ir.co.sadad.noticeapi.services.NoticeService;
import ir.co.sadad.noticeapi.services.ReactiveConsumerService;
import ir.co.sadad.noticeapi.services.ReactiveProducerService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

import static ir.co.sadad.noticeapi.configs.Constants.SSN;

@RestController
@RequestMapping(path = "${v1API}/message")
@RequiredArgsConstructor
public class NoticeController {

    private final ReactiveProducerService producerService;

    private final ReactiveConsumerService consumerService;

    private final NoticeService noticeService;


    @SneakyThrows
    @PostMapping(value = "/kafka/single-send", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<Void> sendSingleNoticeToKafka(@RequestBody SendSingleNoticeReqDto singleNoticeReqDto) throws InterruptedException {
        producerService.send(singleNoticeReqDto);
//        consumerService.run();
        return new ResponseEntity<>(HttpStatus.OK);
//                .map(r -> ResponseEntity.ok().<Void>build())
//                .defaultIfEmpty(ResponseEntity.notFound().build());
//    }

    }

    @GetMapping(value = "/kafka/consume")
    public ResponseEntity<Void> consumeData() {
        consumerService.consumeNotificationDTO().subscribe();
        return new ResponseEntity<>(HttpStatus.OK);
//                .map(r -> ResponseEntity.ok().<Void>build())
//                .defaultIfEmpty(ResponseEntity.notFound().build());
//    }

    }

    //this is just for test
    @PostMapping(value = "/kafka/sample")
    public Mono<ResponseEntity<SendSingleNoticeResDto>> sendSingle(@RequestBody SendSingleNoticeReqDto singleNoticeReqDto) {
        return noticeService.sendSingleNotice(singleNoticeReqDto).map((res -> {

            return ResponseEntity.ok()
                    .body(res);
        }));
    }

    @Operation(summary = "سرویس ارسال پیام کمپین",
            description = "این سرویس لیست کدهای ملی را به صورت فایل گرفته و پیام کمپین را برای آنها ذخیره مینماید.")
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = SendCampaignNoticeResDto.class)))
    @PostMapping(value = "/campaign", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public Mono<ResponseEntity<SendCampaignNoticeResDto>> sendCampaign(@RequestHeader(name = HttpHeaders.AUTHORIZATION) String authToken,
                                                                       @RequestPart("message") SendCampaignNoticeReqDto campaignNoticeReqDto,
                                                                       @RequestPart("file") FilePart filePart) {
        return noticeService.sendCampaignNotice(campaignNoticeReqDto, filePart).map((res -> ResponseEntity.ok().body(res)));
    }

    @Operation(summary = "سرویس لیست پیام های کاربر",
            description = "این سرویس پیام های کاربر را به صورت کلی یا بر اساس تایپ نمایش میدهد.",
            responses = {
                    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = UserNoticeListResDto.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid type"),
            })
    @GetMapping(value = "/notifications")
    public Mono<ResponseEntity<UserNoticeListResDto>> getNotifications(@RequestHeader(name = HttpHeaders.AUTHORIZATION) String authToken,
                                                                       @RequestHeader(SSN) @Parameter(hidden = true) String ssn,
                                                                       @RequestParam(value = "type", required = false) String type) {
        return noticeService.userNoticeList(ssn, type).map((res -> ResponseEntity.ok().body(res)));
    }

    @Operation(summary = "سرویس دریافت آخرین پیام مشاهده شده",
            description = "این سرویس شناسه آخرین پیام مشاهده شده توسط کاربر مربوطه را ذخیره مینماید.")
    @PutMapping(value = "/lastSeen")
    public Mono<ResponseEntity<UserNoticeListResDto>> getLastSeenNotification(@RequestHeader(name = HttpHeaders.AUTHORIZATION) String authToken,
                                                                              @RequestHeader(SSN) @Parameter(hidden = true) String ssn,
                                                                              @RequestParam("lastSeenId") String lastSeenId) {
        return noticeService.UserLastSeenId(ssn, lastSeenId).map((res -> ResponseEntity.ok().body(res)));
    }

    @Operation(summary = "سرویس تعداد پیام های خوانده نشده",
            description = "این سرویس تعداد پیام های خوانده نشده کاربر را برمیگرداند.")
    @GetMapping(value = "/count")
    public Mono<ResponseEntity<UnreadNoticeCountResDto>> getNotificationCount(@RequestHeader(name = HttpHeaders.AUTHORIZATION) String authToken,
                                                                              @RequestHeader(SSN) @Parameter(hidden = true) String ssn) {
        return noticeService.unreadNoticeCount(ssn).map((res -> ResponseEntity.ok().body(res)));
    }

//    @DeleteMapping(value = "/delete")
//    public Mono<ResponseEntity<UserNotification>> deleteSingleNotice(@RequestParam("ssn") String ssn,
//                                                                     @RequestParam("notificationId") String notificationId) {
//        return noticeService.deleteSingleNotice(ssn, notificationId).map(res -> ResponseEntity.ok().body(res));
//    }

    @Operation(summary = "سرویس حذف پیام/پیام ها",
            description = "این سرویس پیام های انتخابی کاربر را حذف مینماید.")
    @PutMapping(value = "/multi-delete")
    public Mono<ResponseEntity<UserNotification>> deleteMultiNotice(@RequestHeader(name = HttpHeaders.AUTHORIZATION) String authToken,
                                                                    @RequestHeader(SSN) @Parameter(hidden = true) String ssn,
                                                                    @RequestBody List<String> notificationIdList) {
        return noticeService.deleteMultiNotice(ssn, notificationIdList).map(res -> ResponseEntity.ok().body(res));
    }
}
