package ir.co.sadad.noticeapi.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import ir.co.sadad.noticeapi.dtos.*;
import ir.co.sadad.noticeapi.services.*;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import javax.validation.Valid;

import static ir.co.sadad.noticeapi.configs.Constants.SSN;
import static ir.co.sadad.noticeapi.configs.Constants.USER_AGENT;

@RestController
@RequestMapping(path = "${v1API}/message")
@RequiredArgsConstructor
public class NoticeController {

    private final ReactiveProducerService producerService;

    private final NoticeMidnightJobService jobService;

    private final NoticeService noticeService;

    private final PanelNoticeService panelNoticeService;

    private final ThirdPartyNotificationService thirdPartyNotificationService;


    @SneakyThrows
    @PostMapping(value = "/kafka/transaction", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<Void> sendTransactionToKafka(@RequestHeader(name = HttpHeaders.AUTHORIZATION) String authToken,
                                                       @RequestBody @Valid TransactionNoticeReqDto transactionNoticeReqDto) {
        producerService.send(transactionNoticeReqDto);
        return new ResponseEntity<>(HttpStatus.OK);

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
                                                                       @RequestHeader(USER_AGENT) String userAgent,
                                                                       @RequestParam(value = "page") int page,
                                                                       @RequestParam(value = "notificationType") String notificationType) {
        return noticeService.userNoticeList(ssn, notificationType, page, userAgent).map((res -> ResponseEntity.ok().body(res)));
    }

    @Operation(summary = "سرویس دریافت آخرین پیام مشاهده شده",
            description = "این سرویس creationDate آخرین پیام مشاهده شده توسط کاربر مربوطه را ذخیره و تعداد پیام های خوانده نشده را صفر مینماید.")
    @PutMapping(value = "/lastSeen")
    public Mono<ResponseEntity<LastSeenResDto>> getLastSeenNotification(@RequestHeader(name = HttpHeaders.AUTHORIZATION) String authToken,
                                                                        @RequestHeader(SSN) @Parameter(hidden = true) String ssn,
                                                                        @RequestBody LastSeenReqDto lastSeenReqDto) {
        return noticeService.userLastSeenId(ssn, lastSeenReqDto.getLastSeenCampaign(), lastSeenReqDto.getLastSeenTransaction())
                .map((res -> ResponseEntity.ok().body(res)));
    }

    @Operation(summary = "سرویس تعداد پیام های خوانده نشده",
            description = "این سرویس تعداد پیام های خوانده نشده کاربر را برمیگرداند.")
    @GetMapping(value = "/unread-count")
    public Mono<ResponseEntity<UnreadNoticeCountResDto>> getNotificationCount(@RequestHeader(name = HttpHeaders.AUTHORIZATION) String authToken,
                                                                              @RequestHeader(USER_AGENT) String userAgent,
                                                                              @RequestHeader(SSN) @Parameter(hidden = true) String ssn) {
        return noticeService.unreadNoticeCount(ssn, userAgent).map((res -> ResponseEntity.ok().body(res)));
    }


    @Operation(summary = "سرویس حذف پیام/پیام ها",
            description = "این سرویس پیام های انتخابی کاربر را حذف مینماید. در صورتی که پیامی وارد نشود، کل پیام های مربوط به نوع اعلان حذف خواهد شد")
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    @DeleteMapping(value = "/delete")
    public Mono<ResponseEntity<Void>> deleteMultiNotice(@RequestHeader(name = HttpHeaders.AUTHORIZATION) String authToken,
                                                        @RequestHeader(SSN) @Parameter(hidden = true) String ssn,
                                                        @RequestBody @Valid DeleteNoticeReqDto deleteNoticeReqDto) {
        return noticeService.deleteMultiNotice(ssn, deleteNoticeReqDto).map(res -> ResponseEntity.noContent().build());
    }

//    @Operation(summary = "سرویس کلیر کردن پیام های خوانده نشده",
//            description = "این سرویس تعداد پیام های خوانده نشده کاربر را صفر خواهد کرد.")
//    @ResponseStatus(value = HttpStatus.NO_CONTENT)
//    @GetMapping(value = "/clear")
//    public Mono<ResponseEntity<Void>> getClearCount(@RequestHeader(name = HttpHeaders.AUTHORIZATION) String authToken,
//                                                                              @RequestHeader(SSN) @Parameter(hidden = true) String ssn) {
//        return noticeService.clearUnreadCount(ssn).map((res -> ResponseEntity.noContent().build()));
//    }


    @PostMapping(value = "/delete-job")
    public void resetCountJob() {
        jobService.resetNoticeCountJob();
        new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping(value = "/push-job")
    public void sendPushJob() {
        jobService.pushNotificationBasedOnActivationDate().subscribe();
        new ResponseEntity<>(HttpStatus.OK);
    }

    /*--------------------panel Rest api--------------------*/

    @Operation(summary = "سرویس ارسال پیام کمپین",
            description = "این سرویس لیست کدهای ملی را به صورت فایل گرفته و پیام کمپین را برای آنها ذخیره مینماید.")
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = SendCampaignNoticeResDto.class)))
    @PostMapping(value = "/campaign", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public Mono<ResponseEntity<SendCampaignNoticeResDto>> sendCampaign(@RequestHeader(name = HttpHeaders.AUTHORIZATION) String authToken,
                                                                       @RequestHeader(SSN) String ssn,
                                                                       @RequestPart("message") @Valid SendCampaignNoticeReqDto campaignNoticeReqDto,
                                                                       @RequestPart(value = "file", required = false) FilePart filePart) {
        campaignNoticeReqDto.setSsn(ssn);
        return panelNoticeService.sendCampaignNotice(campaignNoticeReqDto, filePart).map(res -> ResponseEntity.ok().body(res));
    }


    @Operation(summary = "سرویس بروز رسانی پیام کمپین")
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    @PutMapping(value = "/update")
    public Mono<ResponseEntity<Void>> updateCampaignNotification(@RequestHeader(name = HttpHeaders.AUTHORIZATION) String authToken,
                                                                 @RequestHeader(SSN) String ssn,
                                                                 @RequestBody UpdateCampaignNoticeDto campaignNoticeDto) {
        return panelNoticeService.updateCampaignMessage(campaignNoticeDto, ssn)
                .map(notification -> ResponseEntity.noContent().build());
    }

    @Operation(summary = "سرویس حذف پیام کمپین")
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    @DeleteMapping(value = "/delete-message/{creationDate}")
    public Mono<ResponseEntity<Void>> deleteCampaignNotification(@RequestHeader(name = HttpHeaders.AUTHORIZATION) String authToken,
                                                                 @PathVariable("creationDate") Long creationDate) {
        return panelNoticeService.deleteCampaignMessage(creationDate)
                .map(notification -> ResponseEntity.noContent().build());
    }

    @Operation(summary = "سرویس لیست پیام های کمپین")
    @GetMapping(value = "/list")
    public Mono<ResponseEntity<ListOfCampaignResDto>> getCampaignList(@RequestHeader(name = HttpHeaders.AUTHORIZATION) String authToken,
                                                                      @RequestParam("pageNumber") String pageNumber,
                                                                      @RequestParam("pageSize") String pageSize,
                                                                      @RequestParam(name = "platform", required = false) String platform,
                                                                      @RequestParam(name = "title", required = false) String title,
                                                                      @RequestParam(name = "type", required = false) String type,
                                                                      @RequestParam(name = "date_gt", required = false) String dateFrom,
                                                                      @RequestParam(name = "date_lt", required = false) String dateTo,
                                                                      @RequestParam(name = "sort", required = false) String sort,
                                                                      @RequestParam(name = "sortBy", required = false) String sortBy) {
        PanelNoticeListReqDto reqList = PanelNoticeListReqDto.builder()
                .pageNumber(Integer.parseInt(pageNumber))
                .pageSize(Integer.parseInt(pageSize))
                .platform(platform)
                .title(title)
                .type(type)
                .dateFrom(dateFrom)
                .dateTo(dateTo)
                .sortBy(sortBy)
                .sort(sort).build();
        return panelNoticeService.campaignList(reqList).map((res -> ResponseEntity.ok().body(res)));
    }

    @Operation(summary = "سرویس نمایش لیست کدملی های ناموفق")
    @GetMapping(value = "/failure/{creationDate}")
    public Mono<ResponseEntity<FailureListDto>> failureNotifications(@RequestHeader(name = HttpHeaders.AUTHORIZATION) String authToken,
                                                                     @PathVariable("creationDate") Long creationDate) {
        return panelNoticeService.failureNotifications(creationDate).map(res -> ResponseEntity.ok().body(res));
    }


    /*--------------------ThirdParty api--------------------*/

    @Operation(summary = "سرویس ارسال اعلان از طریق سرویس های ثالث",
            description = "این سرویس بصورت api در اختیار سرویسهای دیگر قرار میگیرد تا بتوانند نتیجه عملیات خود را در قالب اعلان نیز برای کاربر مربوطه ارسال تمایند.")
    @PostMapping(value = "/third-party")
    public Mono<ResponseEntity<sendThirdPartyResDto>> sendThirdPartyNotice(@RequestHeader(name = HttpHeaders.AUTHORIZATION) String authToken,
                                                                           @RequestHeader(SSN) String clientNationalCode,
                                                                           @RequestBody @Valid SendCampaignNoticeReqDto thirdPartyNoticeReqDto) {
        return thirdPartyNotificationService.sendThirdPartyNotice(authToken, clientNationalCode, thirdPartyNoticeReqDto)
                .map(res -> ResponseEntity.ok().body(res));
    }
}
