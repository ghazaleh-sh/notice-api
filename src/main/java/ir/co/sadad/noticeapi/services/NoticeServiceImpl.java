package ir.co.sadad.noticeapi.services;

import ir.co.sadad.noticeapi.dtos.*;
import ir.co.sadad.noticeapi.models.Notification;
import ir.co.sadad.noticeapi.models.UserNotification;
import ir.co.sadad.noticeapi.repositories.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.BsonBinarySubType;
import org.bson.types.Binary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class NoticeServiceImpl implements NoticeService {

    private final NotificationRepository notificationRepository;

    @Override
    public Mono<SendSingleNoticeResDto> sendSingleNotice(SendSingleNoticeReqDto singleNoticeReqDto) {
        SendSingleNoticeResDto res = new SendSingleNoticeResDto();

        return Mono
                .just(singleNoticeReqDto)
                .flatMap(p -> notificationRepository.insert(Notification
                        .builder()
                        .title(p.getTitle())
                        .description(p.getDescription())
                        .date(p.getDate())
                        .type("1")
                        .build()))
                .map(notif -> {
                    res.setId(notif.getId());
                    res.setSsn("000000");
                    res.setStatus("success");
                    return res;
                });
    }

    @Override
    public Flux<SendCampaignNoticeResDto> sendCampaignNotice(SendCampaignNoticeReqDto campaignNoticeReqDto) {
        return null;
    }

    @Override
    public Flux<UserNoticeListResDto> userNoticeList() {
        return null;
    }

    @Override
    public void UserLastSeenId(String lastSeenId) {

    }

    @Override
    public Flux<UnreadNoticeCountResDto> unreadNoticeCount() {
        return null;
    }

    @Override
    public void deleteSingleNotice(String notificaionId) {

    }

    @Override
    public void deleteMultiNotice(List<String> notificaionIdList) {

    }
}
