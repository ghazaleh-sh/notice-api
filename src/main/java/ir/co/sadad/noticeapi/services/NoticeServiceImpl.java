package ir.co.sadad.noticeapi.services;

import ir.co.sadad.noticeapi.dtos.*;
import ir.co.sadad.noticeapi.models.Notification;
import ir.co.sadad.noticeapi.models.UserNotification;
import ir.co.sadad.noticeapi.repositories.NotificationRepository;
import ir.co.sadad.noticeapi.repositories.UserNotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class NoticeServiceImpl implements NoticeService {

    private final NotificationRepository notificationRepository;

    private final UserNotificationRepository userNotificationRepository;

    //just sample - we should use kafka
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
    public Mono<SendCampaignNoticeResDto> sendCampaignNotice(SendCampaignNoticeReqDto campaignNoticeReqDto) {
        //TODO? ssn input is as a file. so, convert ssn file to a list<String> then iterate

        SendCampaignNoticeResDto res = new SendCampaignNoticeResDto();

        List<String> noticeRes = new ArrayList<>();
        int success = 0;
        int failure = 0;

        List<String> notificationIdList = new ArrayList<>();
        String currentNotificationId = Mono
                .just(campaignNoticeReqDto)
                .flatMap(camp -> notificationRepository.save(Notification
                        .builder()
                        .description(camp.getDescription())
                        .title(camp.getTitle())
                        .date(camp.getDate())
                        .type("2")
                        .build()))
                .map(noo-> noo.getId()).toString();

        notificationIdList.add(currentNotificationId);


        return Flux.fromIterable(campaignNoticeReqDto.getSsn())
                .flatMap(currentSsn ->
                        userNotificationRepository.findBySsn(currentSsn)
                                .flatMap(userNotif -> {
                                    List<String> notifsOfUser = userNotif.getNotificationsId();
                                    notifsOfUser.add(currentNotificationId);
                                    userNotif.setNotificationsId(notifsOfUser);
                                    userNotif.setRemainNotificationCount(userNotif.getRemainNotificationCount() + 1L);
                                    log.info("user saved successfully with id and remain: "+ userNotif.getId()+" "+userNotif.getRemainNotificationCount());
                                    return userNotificationRepository.save(userNotif);
                                })
                                .switchIfEmpty(Mono.defer(()->saveUser(currentSsn, notificationIdList)))
                                .doOnSuccess(savedUser -> {
                                    res.setSuccess(String.valueOf(success + 1));
                                    log.info("savedUser is added into res with success-number: "+res.getSuccess());
//                                    return Mono.justOrEmpty(res);
                                })
                                .doOnError(e -> {
                                    res.setFailure(String.valueOf(failure + 1));
                                    noticeRes.add(currentSsn);
                                    res.setFailureResults(noticeRes);
                                    log.info("-----------error is: "+ e.getMessage());
                                }))
                                .then(Mono.justOrEmpty(res));
    }

    private Mono<UserNotification> saveUser(String ssn, List<String> notificationsId) {
        return userNotificationRepository.insert(UserNotification
                .builder()
                .ssn(ssn)
                .notificationsId(notificationsId)
                .remainNotificationCount(1L)
                .lastSeenNotificationId("")
                .previousNotificationId("")
                .build());

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
