package ir.co.sadad.noticeapi.services;

import ir.co.sadad.noticeapi.dtos.PushNotificationReqDto;
import ir.co.sadad.noticeapi.dtos.SendCampaignNoticeReqDto;
import ir.co.sadad.noticeapi.dtos.sendThirdPartyResDto;
import ir.co.sadad.noticeapi.enums.NoticeType;
import ir.co.sadad.noticeapi.enums.NotificationStatus;
import ir.co.sadad.noticeapi.enums.Platform;
import ir.co.sadad.noticeapi.exceptions.ValidationException;
import ir.co.sadad.noticeapi.models.Notification;
import ir.co.sadad.noticeapi.repositories.NotificationRepository;
import ir.co.sadad.noticeapi.repositories.UserNotificationRepository;
import ir.co.sadad.noticeapi.services.utilities.Utilities;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class ThirdPartyNotificationService {

    private final UserNotificationRepository userNotificationRepository;

    private final NotificationRepository notificationRepository;

    private final PanelNoticeService panelNoticeService;

    private final ModelMapper modelMapper;

    private final PushNotificationProviderService pushNotificationService;


    public Mono<sendThirdPartyResDto> sendThirdPartyNotice(String authToken, String clientNationalCode, SendCampaignNoticeReqDto thirdPartyNoticeReqDto) {
        sendThirdPartyResDto res = new sendThirdPartyResDto();
        return Mono.just(thirdPartyNoticeReqDto)
                .flatMap(req -> saveThirdPartyNotification(req, clientNationalCode))
                .flatMap(savedNotification ->
                        userNotificationRepository.findBySsn(thirdPartyNoticeReqDto.getSsn())
                                .flatMap(userNotif -> {
                                    List<Long> campNotifsOfUser = userNotif != null && userNotif.getNotificationCampaignsCreateDate() != null
                                            ? userNotif.getNotificationCampaignsCreateDate() : new ArrayList<>();
                                    campNotifsOfUser.add(savedNotification.getCreationDate());
                                    assert userNotif != null;
                                    userNotif.setNotificationCampaignsCreateDate(campNotifsOfUser);
                                    userNotif.setRemainNotificationCount(userNotif.getRemainNotificationCount() + 1);
                                    userNotif.setNotificationCount(userNotif.getNotificationCount() + 1);
                                    return userNotificationRepository.save(userNotif);

                                })
                                .switchIfEmpty(Mono.defer(() -> panelNoticeService.saveUser(thirdPartyNoticeReqDto.getSsn(), savedNotification.getCreationDate())))
                                .doOnSuccess(savedUser -> res.setNotificationId(savedNotification.getCreationDate()))
                )
                .then(Mono.justOrEmpty(res));

    }

    private Mono<Notification> saveThirdPartyNotification(SendCampaignNoticeReqDto noticeReqDto, String clientNationalCode) {
        if (noticeReqDto.getPushNotification().compareTo(true) == 0) {
            PushNotificationReqDto pushReqDto = new PushNotificationReqDto();
            modelMapper.map(noticeReqDto, pushReqDto);
            pushReqDto.setSuccessSsn(List.of(clientNationalCode));

            pushNotificationService.singlePushNotification(pushReqDto)
                    .subscribe(); // Fire-and-forget: This triggers the execution but immediately "forgets", triggers the HTTP request without waiting for its result, continuing the flow immediately.
            //No execution happens until something subscribes to the reactive source (like a Mono or Flux)
        }

        return Mono
                .just(noticeReqDto)
                .flatMap(tpNotice -> notificationRepository.insert(Notification
                        .builder()
                        .creationDate(System.currentTimeMillis())
                        .description(tpNotice.getDescription())
                        .title(tpNotice.getTitle())
                        .type(NoticeType.CAMPAIGN.getValue())
                        .platform(tpNotice.getPlatform() != null ? Platform.valueOf(tpNotice.getPlatform())
                                : Platform.ALL)
                        .createdBy(clientNationalCode)
                        .creationDateUTC(Utilities.getCurrentUTC())
                        .status(NotificationStatus.ACTIVE)
                        .activationDate(!(tpNotice.getActivationDate().isEmpty()) ?
                                tpNotice.getActivationDate().split("T")[0] : null)
                        .hyperlink(tpNotice.getHyperlink())
                        .build()))
                .onErrorMap(throwable -> new ValidationException(throwable.getMessage(), "error.on.save.notification"));
    }

}
