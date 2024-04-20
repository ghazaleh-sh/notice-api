package ir.co.sadad.noticeapi.services;

import ir.co.sadad.noticeapi.dtos.ListOfCampaignResDto;
import ir.co.sadad.noticeapi.dtos.UpdateCampaignNoticeDto;
import ir.co.sadad.noticeapi.models.Notification;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * service related to notice-panel and is used by specific baam users
 *
 * @author g.shahrokhabadi
 * created on 2023/06/18
 */
public interface PanelNoticeService {

    Mono<Notification> updateCampaignMessage(UpdateCampaignNoticeDto updateCampaignDto);

    Mono<Void> deleteCampaignMessage(Long notificationId);

    Mono<ListOfCampaignResDto> campaignList();

}
