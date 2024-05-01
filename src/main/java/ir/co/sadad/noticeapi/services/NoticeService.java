package ir.co.sadad.noticeapi.services;

import ir.co.sadad.noticeapi.dtos.*;
import ir.co.sadad.noticeapi.models.UserNotification;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Mono;


/**
 * service for sending notices to users and getting user notices data
 *
 * @author g.shahrokhabadi
 * created on 2022/12/28
 */
public interface NoticeService {

    Mono<SendCampaignNoticeResDto> sendCampaignNotice(SendCampaignNoticeReqDto campaignNoticeReqDto, FilePart file);

    Mono<UserNoticeListResDto> userNoticeList(String ssn, String type, int page, String userAgent);

    Mono<LastSeenResDto> UserLastSeenId(String ssn, Long lastSeenCampaign, Long lastSeenTransaction);

    Mono<UnreadNoticeCountResDto> unreadNoticeCount(String ssn);

    Mono<UserNotification> deleteMultiNotice(String ssn, DeleteNoticeReqDto deleteNoticeReqDto);

}
