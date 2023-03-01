package ir.co.sadad.noticeapi.services;

import ir.co.sadad.noticeapi.dtos.*;
import ir.co.sadad.noticeapi.models.Notification;
import ir.co.sadad.noticeapi.models.UserNotification;
import ir.co.sadad.noticeapi.repositories.NotificationRepository;
import ir.co.sadad.noticeapi.repositories.UserNotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.Charsets;
import org.apache.commons.io.IOUtils;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class NoticeServiceImpl implements NoticeService {

    private final NotificationRepository notificationRepository;

    private final UserNotificationRepository userNotificationRepository;

    private List<String> ssnList = new ArrayList<>();

    private final AtomicInteger success = new AtomicInteger();
    private final AtomicInteger failure = new AtomicInteger();

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
    @SneakyThrows
    public Mono<SendCampaignNoticeResDto> sendCampaignNotice(SendCampaignNoticeReqDto campaignNoticeReqDto, FilePart file) {
        SendCampaignNoticeResDto res = new SendCampaignNoticeResDto();
        String fileLocation = "/";

        List<String> noticeRes = new ArrayList<>();
        success.set(0);
        failure.set(0);

        return Mono.just(file)
                .flatMap(filePart -> DataBufferUtils.join(filePart.content())
                        .map(dataBuffer -> {
                            try {
                                ssnList = IOUtils.readLines(dataBuffer.asInputStream(), Charsets.UTF_8);
                                return ssnList;
                            } catch (IOException e) {
                                e.printStackTrace();
                                return Mono.empty();
                            }
                        })
                        .switchIfEmpty(Mono.defer(() -> readAllNationalCode(fileLocation))))
                .flatMap(o -> saveNotification(campaignNoticeReqDto))
                .flatMapMany(savedNotification -> Flux.fromIterable(ssnList)
                        .flatMap(currentSsn ->
                                userNotificationRepository.findBySsn(currentSsn)
                                        .flatMap(userNotif -> {
                                            List<Notification> notifsOfUser = userNotif != null ? userNotif.getNotifications() : new ArrayList<>();
                                            notifsOfUser.add(savedNotification);
                                            assert userNotif != null;
                                            userNotif.setNotifications(notifsOfUser);
                                            userNotif.setRemainNotificationCount(userNotif.getRemainNotificationCount() + 1L);
//                                            log.info("user saved successfully with id and remain: " + userNotif.getId() + " " + userNotif.getRemainNotificationCount());
                                            success.getAndIncrement();
                                            return userNotificationRepository.save(userNotif);

                                        })
                                        .switchIfEmpty(Mono.defer(() -> saveUser(currentSsn, savedNotification)))
                                        .doOnSuccess(savedUser -> {
                                            res.setSuccess(String.valueOf(success.get()));
//                                            log.info("savedUser is added into res with success-number: " + res.getSuccess());
                                        })
                                        .doOnError(e -> {
                                            failure.getAndIncrement();
                                            res.setFailure(String.valueOf(failure.get()));
                                            noticeRes.add(currentSsn);
                                            res.setFailureResults(noticeRes);
                                            log.info("-----------error is: " + e.getMessage());
                                        })))
                .then(Mono.justOrEmpty(res));
    }

    @SneakyThrows
    private <T> Mono<T> readAllNationalCode(String fileLocation) {
        return Mono.empty();
    }

    private Mono<Notification> saveNotification(SendCampaignNoticeReqDto campaignNoticeReqDto) {
        return Mono
                .just(campaignNoticeReqDto)
                .flatMap(cammm -> notificationRepository.findByDateAndType(cammm.getDate(), "2"))
                .map(savedNot -> savedNot).cache()
                .switchIfEmpty(Mono.defer(() -> notificationRepository.insert(Notification
                        .builder()
                        .description(campaignNoticeReqDto.getDescription())
                        .title(campaignNoticeReqDto.getTitle())
                        .date(campaignNoticeReqDto.getDate())
                        .type("2")
                        .build())));
    }

    private Mono<UserNotification> saveUser(String ssn, Notification savedNotification) {
        List<Notification> notifsOfUser = new ArrayList<>();
        notifsOfUser.add(savedNotification);
        success.getAndIncrement();

        return userNotificationRepository.insert(UserNotification
                .builder()
                .ssn(ssn)
                .notifications(notifsOfUser)
                .remainNotificationCount(1L)
                .lastSeenNotificationId("")
                .previousNotificationId("")
                .build());
    }

    @Override
    public Mono<UserNoticeListResDto> userNoticeList(String ssn, String type) {
        UserNoticeListResDto res = new UserNoticeListResDto();
        res.setSsn(ssn);

        return userNotificationRepository.findBySsn(ssn)
                .map(userNotification -> {
                    if(type!= null)
                        res.setNotifications(userNotification.getNotifications().stream()
                            .filter(notification -> notification.getType().equals(type))
                            .collect(Collectors.toList()));
                    else
                        res.setNotifications(userNotification.getNotifications());
                    res.setLastSeenId(userNotification.getLastSeenNotificationId());
                    return res;
                });
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
