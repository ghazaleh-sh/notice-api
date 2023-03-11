package ir.co.sadad.noticeapi.services;

import ir.co.sadad.noticeapi.dtos.*;
import ir.co.sadad.noticeapi.exceptions.GeneralException;
import ir.co.sadad.noticeapi.exceptions.ValidationException;
import ir.co.sadad.noticeapi.models.Notification;
import ir.co.sadad.noticeapi.models.UserNotification;
import ir.co.sadad.noticeapi.repositories.NotificationRepository;
import ir.co.sadad.noticeapi.repositories.UserNotificationRepository;
import ir.co.sadad.noticeapi.validations.NationalCodeValidator;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.Charsets;
import org.apache.commons.io.IOUtils;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
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
                .flatMap(notif -> {
                    res.setId(notif.getId());
                    res.setSsn("000000");
                    res.setStatus("success");
                    return Mono.just(res);
                });
    }

    @Override
    @SneakyThrows
    public Mono<SendCampaignNoticeResDto> sendCampaignNotice(SendCampaignNoticeReqDto campaignNoticeReqDto, FilePart file) {
        SendCampaignNoticeResDto res = new SendCampaignNoticeResDto();
        String fileLocation = "/";

        List<String> failRes = new ArrayList<>();
        success.set(0);
        failure.set(0);

        return Mono.just(file)
                .flatMap(filePart -> DataBufferUtils.join(filePart.content())
                        .map(dataBuffer -> {
                            try {
                                ssnList = IOUtils.readLines(dataBuffer.asInputStream(), Charsets.UTF_8);
                                List<String> invalidSsn = new ArrayList<>();
                                ssnList.forEach(s -> {
                                    if (!NationalCodeValidator.isValid(s)) {
                                        failure.getAndIncrement();
                                        failRes.add(s);
                                        invalidSsn.add(s);
                                    }
                                });
                                ssnList.removeAll(invalidSsn);
                                return ssnList;

                            } catch (IOException e) {
                                e.printStackTrace();
                                return Mono.error(new GeneralException("ssn.file.invalid", HttpStatus.BAD_REQUEST));
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
                                            success.getAndIncrement();
                                            return userNotificationRepository.save(userNotif);

                                        })
                                        .switchIfEmpty(Mono.defer(() -> saveUser(currentSsn, savedNotification)))
                                        .doOnSuccess(savedUser -> {
                                            res.setNotificationId(savedNotification.getId());
                                            res.setSuccess(String.valueOf(success.get()));
                                            res.setFailure(String.valueOf(failure.get()));
                                            res.setFailureResults(failRes);
                                        })
                                        .onErrorMap(throwable -> {
                                            res.setNotificationId(savedNotification.getId());
                                            failure.getAndIncrement();
                                            res.setFailure(String.valueOf(failure.get()));
                                            failRes.add(currentSsn);
                                            res.setFailureResults(failRes);
                                            return new GeneralException(throwable.getMessage(), "error.on.map.user.notification");
                                        })
                                        .doOnError(e -> {
                                            res.setNotificationId(savedNotification.getId());
                                            failure.getAndIncrement();
                                            res.setFailure(String.valueOf(failure.get()));
                                            failRes.add(currentSsn);
                                            res.setFailureResults(failRes);
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
                .cache()
                .switchIfEmpty(Mono.defer(() -> notificationRepository.insert(Notification
                        .builder()
                        .description(campaignNoticeReqDto.getDescription())
                        .title(campaignNoticeReqDto.getTitle())
                        .date(campaignNoticeReqDto.getDate())
                        .type("2")
                        .build())))
//                        .handle((notif, sink) -> {
//                            if (notif == null)
//                                sink.error(new GeneralException("error.on.save.notification"));
//                        }))
                .onErrorMap(throwable -> new ValidationException(throwable.getMessage(), "error.on.save.notification"))
                // just to see what is being emitted
                .log();
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
                .build())
                .onErrorMap(throwable -> new GeneralException(throwable.getMessage(), "error.on.save.user.notification"))
                // just to see what is being emitted
                .log();
    }

    @Override
    public Mono<UserNoticeListResDto> userNoticeList(String ssn, String type) {
        UserNoticeListResDto res = new UserNoticeListResDto();
        res.setSsn(ssn);

        return userNotificationRepository.findBySsn(ssn)
                .flatMap(userNotification -> {
                    if (type != null)
                        res.setNotifications(userNotification.getNotifications().stream()
                                .filter(notification -> notification.getType().equals(type))
                                .collect(Collectors.toList()));
                    else
                        res.setNotifications(userNotification.getNotifications());

                    res.setLastSeenId(userNotification.getLastSeenNotificationId());
                    return Mono.just(res);
                })
                .switchIfEmpty(Mono.error(new GeneralException("ssn.not.find", HttpStatus.NOT_FOUND)));
    }

    @Override
    public Mono<UserNoticeListResDto> UserLastSeenId(String ssn, String lastSeenId) {
        UserNoticeListResDto result = new UserNoticeListResDto();

        return userNotificationRepository.findBySsn(ssn)
                .map(res -> new UserNotification(res.getId(),
                        res.getSsn(),
                        res.getNotifications(),
                        lastSeenId,
                        !res.getLastSeenNotificationId().equals("") ? res.getLastSeenNotificationId(): res.getPreviousNotificationId(),
                        Math.abs(res.getRemainNotificationCount() -
                                readiedNoticesCount(res.getNotifications(), res.getLastSeenNotificationId(), lastSeenId))))
                .flatMap(this.userNotificationRepository::save)
                .switchIfEmpty(Mono.error(new GeneralException("ssn.not.find", HttpStatus.NOT_FOUND)))
                .flatMap(user -> {
                    result.setLastSeenId(user.getLastSeenNotificationId());
                    result.setSsn(ssn);
                    return Mono.just(result);
                });

    }
//TODO: write in reactive mode
    private Long readiedNoticesCount(List<Notification> notifs, String preId, String lastSeen) {
        Long startPoint = preId.equals("") ? -1L : null, endPoint = null;

        for(int i=0; i< notifs.size();i++){
            if(startPoint !=null && endPoint!= null)
                break;
            if(notifs.get(i).getId().equals(preId))
                startPoint = (long) i;
            if(notifs.get(i).getId().equals(lastSeen))
                endPoint = (long)i;
        }
        if(startPoint == null || endPoint == null)
            throw new GeneralException("notification.id.not.valid", HttpStatus.BAD_REQUEST);

        if(startPoint > endPoint)
            throw new GeneralException("pre.id.is.newer", HttpStatus.BAD_REQUEST);

        return endPoint - startPoint;
    }

    @Override
    public Mono<UnreadNoticeCountResDto> unreadNoticeCount(String ssn) {
        UnreadNoticeCountResDto res = new UnreadNoticeCountResDto();

        return userNotificationRepository.findBySsn(ssn)
                .flatMap(userNotification -> {
                    res.setNotificationCount(userNotification.getRemainNotificationCount());
                    res.setSsn(ssn);
                    return Mono.just(res);
                })
                .switchIfEmpty(Mono.error(new GeneralException("ssn.not.find", HttpStatus.NOT_FOUND)));
    }

    //not used, because multi delete can delete single notice too.
//    @Override
//    public Mono<UserNotification> deleteSingleNotice(String ssn, String notificationId) {
//        return userNotificationRepository.findBySsn(ssn)
//                .map(res -> {
//                    List<Notification> newList = res.getNotifications().stream()
//                            .filter(notification -> !notification.getId().equals(notificationId))
//                            .collect(Collectors.toList());
////                    log.info("new list is: "+newList);
//                    return new UserNotification(res.getId(),
//                            res.getSsn(),
//                            newList,
//                            res.getLastSeenNotificationId(),
//                            res.getPreviousNotificationId(),
//                            res.getRemainNotificationCount());
//                })
//                .flatMap(this.userNotificationRepository::save);
//    }

    @Override
    public Mono<UserNotification> deleteMultiNotice(String ssn, List<String> notificationIdList) {
        //TODO: reduce unread-message-count based on business

        return userNotificationRepository.findBySsn(ssn)
                .map(res -> {
                    List<Notification> newList = notificationIdList.isEmpty() ? Collections.emptyList() :
                            res.getNotifications().stream()
                                    .filter(notification -> !notificationIdList.contains(notification.getId()))
                                    .collect(Collectors.toList());
                    return new UserNotification(res.getId(),
                            res.getSsn(),
                            newList,
                            res.getLastSeenNotificationId(),
                            res.getPreviousNotificationId(),
                            res.getRemainNotificationCount());
                })
                .switchIfEmpty(Mono.error(new GeneralException("ssn.not.find", HttpStatus.NOT_FOUND)))
                .flatMap(this.userNotificationRepository::save);

    }
}
