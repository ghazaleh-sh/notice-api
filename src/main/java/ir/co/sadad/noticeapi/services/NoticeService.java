package ir.co.sadad.noticeapi.services;

import ir.co.sadad.noticeapi.dtos.DeleteNoticeReqDto;
import ir.co.sadad.noticeapi.dtos.LastSeenResDto;
import ir.co.sadad.noticeapi.dtos.UnreadNoticeCountResDto;
import ir.co.sadad.noticeapi.dtos.UserNoticeListResDto;
import ir.co.sadad.noticeapi.models.UserNotification;
import reactor.core.publisher.Mono;


/**
 * service for sending notices to users and getting user notices data
 *
 * @author g.shahrokhabadi
 * created on 2022/12/28
 */
public interface NoticeService {

    Mono<UserNoticeListResDto> userNoticeList(String ssn, String type, int page, String userAgent);

    Mono<LastSeenResDto> userLastSeenId(String ssn, Long lastSeenCampaign, Long lastSeenTransaction);

    Mono<UnreadNoticeCountResDto> unreadNoticeCount(String ssn, String userAgent);

    Mono<UserNotification> deleteMultiNotice(String ssn, DeleteNoticeReqDto deleteNoticeReqDto);

}
