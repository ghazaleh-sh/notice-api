package ir.co.sadad.noticeapi.services;

import ir.co.sadad.noticeapi.dtos.*;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * a service for sending notices to users and getting user notices data
 *
 * @author g.shahrokhabadi
 * created on 2021/12/28
 */
public interface NoticeService {

    Mono<SendSingleNoticeResDto> sendSingleNotice(SendSingleNoticeReqDto singleNoticeReqDto);

    Mono<SendCampaignNoticeResDto> sendCampaignNotice(SendCampaignNoticeReqDto campaignNoticeReqDto, FilePart file);

    Mono<UserNoticeListResDto> userNoticeList(String ssn, String type);

    void UserLastSeenId(String lastSeenId);

    Flux<UnreadNoticeCountResDto> unreadNoticeCount();

    void deleteSingleNotice(String notificaionId);

    void deleteMultiNotice(List<String> notificaionIdList);
}
