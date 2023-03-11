package ir.co.sadad.noticeapi.services;

import ir.co.sadad.noticeapi.dtos.*;
import ir.co.sadad.noticeapi.models.UserNotification;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * service for sending notices to users and getting user notices data
 *
 * @author g.shahrokhabadi
 * created on 2021/12/28
 */
public interface NoticeService {

    Mono<SendSingleNoticeResDto> sendSingleNotice(SendSingleNoticeReqDto singleNoticeReqDto);

    Mono<SendCampaignNoticeResDto> sendCampaignNotice(SendCampaignNoticeReqDto campaignNoticeReqDto, FilePart file);

    Mono<UserNoticeListResDto> userNoticeList(String ssn, String type);

    Mono<UserNoticeListResDto> UserLastSeenId(String ssn, String lastSeenId);

    Mono<UnreadNoticeCountResDto> unreadNoticeCount(String ssn);

//    Mono<UserNotification> deleteSingleNotice(String ssn, String notificaionId);

    Mono<UserNotification> deleteMultiNotice(String ssn, List<String> notificaionIdList);
}
