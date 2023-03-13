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
                .onErrorMap(throwable -> new ValidationException(throwable.getMessage(), "error.on.save.notification"));
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
                .lastSeenNotificationIndex(0)
                .previousNotificationIndex(-1)
                .build())
                .onErrorMap(throwable -> new GeneralException(throwable.getMessage(), "error.on.save.user.notification"))
                // just to see what is being emitted
                .log();
    }

    @Override
    public Mono<UserNoticeListResDto> userNoticeList(String ssn, String type) {
        UserNoticeListResDto res = new UserNoticeListResDto();
        res.setSsn(ssn);

        /*
        for concerning about backpressure when collecting the list of notifications,
        you can use the Flux.fromIterable method to convert the list to a Flux
         and then apply backpressure operators on it.
         */
        return userNotificationRepository.findBySsn(ssn)
                .flatMap(userNotification -> {
                    Flux<Notification> notificationsFlux = Flux.fromIterable(userNotification.getNotifications());
                    if (type != null) {
                        notificationsFlux = notificationsFlux.filter(notification -> notification.getType().equals(type));
                    }
                    return notificationsFlux
                            .onBackpressureBuffer(20) // buffer up to 20 items
                            .limitRate(20) // limit the rate at which items are emitted to 20
                            .collectList()
                            .doOnNext(notifications -> {
                                res.setNotifications(notifications);
                                res.setLastSeenId(userNotification.getLastSeenNotificationId());
                            })
                            .thenReturn(res);
                })
                .switchIfEmpty(Mono.error(new GeneralException("ssn.not.find", HttpStatus.NOT_FOUND)));
    }

    @Override
    public Mono<UserNoticeListResDto> UserLastSeenId(String ssn, String lastSeenId) {

        return userNotificationRepository.findBySsn(ssn)
                .switchIfEmpty(Mono.error(new GeneralException("ssn.not.find", HttpStatus.NOT_FOUND)))
                .flatMap(res -> {
                    Mono<Integer> lastSeenInxMono = Flux.fromIterable(res.getNotifications())
                            .filter(notification -> notification.getId().equals(lastSeenId))
                            .next()
                            .map(notification -> res.getNotifications().indexOf(notification))
                            .switchIfEmpty(Mono.error(new GeneralException("notification.id.not.valid", HttpStatus.NOT_FOUND)));

                    return lastSeenInxMono.flatMap(lastSeenInx -> {
                        if (res.getLastSeenNotificationIndex() > lastSeenInx)
                            return Mono.error(new GeneralException("pre.id.is.newer", HttpStatus.BAD_REQUEST));

                        return Mono.just(new UserNotification(res.getId(),
                                res.getSsn(),
                                res.getNotifications(),
                                lastSeenId,
                                lastSeenInx,
                                !res.getLastSeenNotificationId().equals("") ? res.getLastSeenNotificationIndex() : res.getPreviousNotificationIndex(),
                                Math.abs(res.getRemainNotificationCount() -
                                        (lastSeenInx - (res.getLastSeenNotificationIndex() == 0 ? res.getPreviousNotificationIndex() : res.getLastSeenNotificationIndex()))
                                )));

                    });
                })
                .flatMap(this.userNotificationRepository::save)
                .map(user -> {
                    UserNoticeListResDto result = new UserNoticeListResDto();
                    result.setLastSeenId(user.getLastSeenNotificationId());
                    result.setSsn(ssn);
                    return result;
                });

    }

    @Override
    public Mono<UnreadNoticeCountResDto> unreadNoticeCount(String ssn) {

        return userNotificationRepository.findBySsn(ssn)
                .flatMap(userNotification -> {
                    UnreadNoticeCountResDto res = new UnreadNoticeCountResDto();
                    res.setNotificationCount(userNotification.getRemainNotificationCount());
                    res.setSsn(ssn);
                    return Mono.just(res);
                })
                .switchIfEmpty(Mono.error(new GeneralException("ssn.not.find", HttpStatus.NOT_FOUND)));
    }


    @Override
    public Mono<UserNotification> deleteMultiNotice(String ssn, List<String> notificationIdList) {
        //TODO: check if deleted message is lastSeenId.So,...
/*
 the filter operation inside the map function is not fully reactive as it involves iterating over a list.
 To make it more reactive, you could use the Flux class instead of a list to filter the notifications.
 */
        return userNotificationRepository.findBySsn(ssn)
                .flatMap(res -> {
                    Flux<Notification> notifications = notificationIdList.isEmpty() ?
                            Flux.empty() :
                            Flux.fromIterable(res.getNotifications()).filter(notification -> !notificationIdList.contains(notification.getId()));

                    if ((notificationIdList.size() - 1) > res.getLastSeenNotificationIndex())
                        return Mono.error(new GeneralException("deleted.count.more.than.read", HttpStatus.BAD_REQUEST));

                    return notifications.collectList().map(newList -> new UserNotification(
                            res.getId(),
                            res.getSsn(),
                            newList,
                            newList.isEmpty() ? "" : res.getLastSeenNotificationId(),
                            newList.isEmpty() ? 0 : res.getLastSeenNotificationIndex() - notificationIdList.size() + 1,
                            newList.isEmpty() ? -1 : setPreviousIndex(res.getPreviousNotificationIndex(), notificationIdList.size()),
                            newList.isEmpty() ? 0 : res.getRemainNotificationCount()
                    ));
                })
                .switchIfEmpty(Mono.error(new GeneralException("ssn.not.find", HttpStatus.NOT_FOUND)))
                .flatMap(this.userNotificationRepository::save);

    }

    private Integer setPreviousIndex(Integer preInx, int size) {
        if ((preInx - size + 1) > -2)
            return preInx - size + 1;
        else
            return -1;

    }
}
