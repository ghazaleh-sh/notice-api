package ir.co.sadad.noticeapi.services;

import ir.co.sadad.noticeapi.dtos.*;
import ir.co.sadad.noticeapi.models.Notification;
import ir.co.sadad.noticeapi.models.UserNotification;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Mono;

/**
 * service related to notice-panel and is used by specific baam users
 *
 * @author g.shahrokhabadi
 * created on 2023/06/18
 */
public interface PanelNoticeService {

    Mono<SendCampaignNoticeResDto> sendCampaignNotice(SendCampaignNoticeReqDto campaignNoticeReqDto, FilePart file);

    Mono<Notification> updateCampaignMessage(UpdateCampaignNoticeDto updateCampaignDto, String ssn);

    Mono<Void> deleteCampaignMessage(Long notificationId);

    Mono<ListOfCampaignResDto> campaignList(PanelNoticeListReqDto reqList);

    Mono<FailureListDto> failureNotifications(Long notificationId);

    Mono<UserNotification> saveUser(String ssn, Long savedNotificationId);

}
