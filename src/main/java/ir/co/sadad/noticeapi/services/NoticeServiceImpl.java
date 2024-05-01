package ir.co.sadad.noticeapi.services;

import ir.co.sadad.noticeapi.dtos.*;
import ir.co.sadad.noticeapi.enums.NoticeType;
import ir.co.sadad.noticeapi.enums.Platform;
import ir.co.sadad.noticeapi.exceptions.GeneralException;
import ir.co.sadad.noticeapi.exceptions.ValidationException;
import ir.co.sadad.noticeapi.models.Notification;
import ir.co.sadad.noticeapi.models.UserNotification;
import ir.co.sadad.noticeapi.repositories.NotificationRepository;
import ir.co.sadad.noticeapi.repositories.UserNotificationRepository;
import ir.co.sadad.noticeapi.services.utilities.Utilities;
import ir.co.sadad.noticeapi.validations.NationalCodeValidator;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.Charsets;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Value;
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
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@RequiredArgsConstructor
@Service
public class NoticeServiceImpl implements NoticeService {

    private final NotificationRepository notificationRepository;

    private final UserNotificationRepository userNotificationRepository;

    private List<String> ssnList = new ArrayList<>();

    private final AtomicInteger success = new AtomicInteger();
    private final AtomicInteger failure = new AtomicInteger();

    @Value("${notifications.page-size}")
    private int pageSize;


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
                                            List<Long> campNotifsOfUser = userNotif != null && userNotif.getNotificationCampaignsCreateDate() != null
                                                    ? userNotif.getNotificationCampaignsCreateDate() : new ArrayList<>();
                                            campNotifsOfUser.add(savedNotification.getCreationDate());
                                            assert userNotif != null;
                                            userNotif.setNotificationCampaignsCreateDate(campNotifsOfUser);
                                            userNotif.setRemainNotificationCount(userNotif.getRemainNotificationCount() + 1);
                                            userNotif.setNotificationCount(userNotif.getNotificationCount() + 1);
                                            success.getAndIncrement();
                                            return userNotificationRepository.save(userNotif);

                                        })
                                        .switchIfEmpty(Mono.defer(() -> saveUser(currentSsn, savedNotification.getCreationDate())))
                                        .doOnSuccess(savedUser -> {
                                            res.setNotificationId(savedNotification.getId());
                                            res.setSuccess(String.valueOf(success.get()));
                                            res.setFailure(String.valueOf(failure.get()));
                                            res.setFailureResults(failRes);
                                        })
                                        .onErrorResume(e -> {
                                            failure.getAndIncrement();
                                            failRes.add(currentSsn);
                                            res.setNotificationId(savedNotification.getId());
                                            res.setFailure(String.valueOf(failure.get()));
                                            res.setFailureResults(failRes);
                                            log.info("-----------error on " + currentSsn + "is: " + e.getMessage());
                                            return Mono.empty();
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
//                .flatMap(camp -> notificationRepository.findByDateAndType(camp.getDate(), "2"))
//                .cache()
                .flatMap(camp -> notificationRepository.insert(Notification
                        .builder()
                        .creationDate(System.currentTimeMillis())
                        .description(camp.getDescription())
                        .title(camp.getTitle())
                        .type(NoticeType.CAMPAIGN.getValue())
                        .platform(Platform.valueOf(camp.getPlatform()))
                        .createdBy(camp.getSsn())
                        .creationDateUTC(Utilities.currentUTCDate())
                        .build()))
                .onErrorMap(throwable -> new ValidationException(throwable.getMessage(), "error.on.save.notification"));
    }

    private Mono<UserNotification> saveUser(String ssn, Long savedNotificationId) {
        List<Long> notifsOfUser = new ArrayList<>();
        notifsOfUser.add(savedNotificationId);
        success.getAndIncrement();

        return userNotificationRepository.insert(UserNotification
                        .builder()
                        .ssn(ssn)
                        .notificationCampaignsCreateDate(notifsOfUser)
                        .lastSeenCampaign(0L)
                        .lastSeenTransaction(0L)
                        .remainNotificationCount(1)
                        .notificationCount(1)
                        .build())
                .onErrorMap(throwable -> new GeneralException(throwable.getMessage(), "error.on.save.user.notification"));
        // just to see what is being emitted
//                .log();
    }

    @Override
    public Mono<UserNoticeListResDto> userNoticeList(String ssn, String type, int page, String userAgent) {
        Platform userCurrentPlatform = Utilities.checkUserAgent(userAgent);

        return userNotificationRepository.findBySsn(ssn)
                .switchIfEmpty(Mono.error(new GeneralException("ssn.not.find", HttpStatus.NOT_FOUND)))
                .flatMap(userNotification -> {

                    Flux<Notification> notificationsFlux;

                    if (type.equals(NoticeType.TRANSACTION.getValue())) {
                        notificationsFlux = Flux.fromIterable(userNotification.getNotificationTransactions());

                    } else if (type.equals(NoticeType.CAMPAIGN.getValue())) {
                        Flux<Long> notificationsIdFlux = Flux.fromIterable(userNotification.getNotificationCampaignsCreateDate());

                        notificationsFlux = notificationsIdFlux
                                //processing each notificationId in parallel
                                .flatMapSequential(notificationId -> {
                                            if (userCurrentPlatform == null)
                                                return notificationRepository.findByCreationDate(notificationId);
                                            else
                                                return notificationRepository.findByCreationDateAndPlatform(notificationId, userCurrentPlatform);
                                        }
                                )
                                .switchIfEmpty(Mono.empty())
                                .filter(Objects::nonNull); // Filter out empty values

                    } else {
                        return Mono.error(new GeneralException("type.not.find", HttpStatus.BAD_REQUEST));
                    }

                    return notificationsFlux
//                            .switchIfEmpty(Flux.empty())
                            .sort((o1, o2) -> (int) (o2.getCreationDate() - o1.getCreationDate()))
//                            .onBackpressureBuffer(20) // buffer up to 20 items
//                            .limitRate(20) // limit the rate at which items are emitted to 20
                            .collectList()
                            .flatMap(notifications -> {

                                int totalItems = notifications.size();
                                // ceil method to round up the argument to the nearest integer value
                                int totalPages = (int) Math.ceil((double) totalItems / pageSize);

                                if (totalPages != 0 && page > totalPages) {
                                    return Mono.error(new GeneralException("page.is.more.than.total", HttpStatus.BAD_REQUEST));
                                }

//                                log.info("totalItems and totalPages : " + totalItems + "  " + totalPages);

                                int startIndex = (page - 1) * pageSize;
                                int endIndex = Math.min(startIndex + pageSize, totalItems);

                                startIndex = Math.max(startIndex, 0);  // Ensure startIndex is not negative
                                endIndex = Math.min(endIndex, totalItems);  // Ensure endIndex does not exceed the list size


                                UserNoticeListResDto res = new UserNoticeListResDto();

                                res.setCurrentPage(page);
                                res.setTotalPages(totalPages);
                                res.setNotifications(notifications.isEmpty() ? Collections.emptyList() : notifications.subList(startIndex, endIndex));
                                res.setLastSeenCampaign(userNotification.getLastSeenCampaign());
                                res.setLastSeenTransaction(userNotification.getLastSeenTransaction());

                                return Mono.just(res);
                            });

                });

    }


    @Override
    public Mono<LastSeenResDto> UserLastSeenId(String ssn, Long lastSeenCampaign, Long lastSeenTransaction) {

        return userNotificationRepository.findBySsn(ssn)
                .switchIfEmpty(Mono.error(new GeneralException("ssn.not.find", HttpStatus.NOT_FOUND)))
                .flatMap(res -> Mono.just(new UserNotification(res.getId(),
                        res.getSsn(),
                        res.getNotificationTransactions(),
                        res.getNotificationCampaignsCreateDate(),
                        lastSeenCampaign != null ? lastSeenCampaign : res.getLastSeenCampaign(),
                        lastSeenTransaction != null ? lastSeenTransaction : res.getLastSeenTransaction(),
                        0,//res.getRemainNotificationCount(),
                        res.getNotificationCount()
                )))
                .flatMap(this.userNotificationRepository::save)
                .map(user -> {
                    LastSeenResDto result = new LastSeenResDto();
                    result.setLastSeenCampaign(user.getLastSeenCampaign());
                    result.setLastSeenTransaction(user.getLastSeenTransaction());
                    return result;
                });
    }


    @Override
    public Mono<UnreadNoticeCountResDto> unreadNoticeCount(String ssn) {

        return userNotificationRepository.findBySsn(ssn)
                .flatMap(userNotification -> {
                    UnreadNoticeCountResDto res = new UnreadNoticeCountResDto();
                    res.setRemainNotificationCount(userNotification.getRemainNotificationCount());
                    res.setLastSeenCampaign(userNotification.getLastSeenCampaign());
                    res.setLastSeenTransaction(userNotification.getLastSeenTransaction());
                    return Mono.just(res);
                })
                .switchIfEmpty(Mono.error(new GeneralException("ssn.not.find", HttpStatus.NOT_FOUND)));
    }


    @Override
    public Mono<UserNotification> deleteMultiNotice(String ssn, DeleteNoticeReqDto deleteDto) {
        return userNotificationRepository.findBySsn(ssn)
                .switchIfEmpty(Mono.error(new GeneralException("ssn.not.find", HttpStatus.NOT_FOUND)))
                .flatMap(userNotification -> {

                    if (deleteDto.getNotificationType().equals(NoticeType.TRANSACTION.getValue())) {
                        Flux<Notification> notificationsFlux = deleteDto.getNotificationsByCreationDate().isEmpty() ?
                                Flux.empty() :
                                Flux.fromIterable(userNotification.getNotificationTransactions())
                                        .filter(notification -> !deleteDto.getNotificationsByCreationDate().contains(notification.getCreationDate()));

                        return notificationsFlux.sort((o1, o2) -> (int) (o2.getCreationDate() - o1.getCreationDate()))
                                .collectList().map(newList -> new UserNotification(
                                        userNotification.getId(),
                                        userNotification.getSsn(),
                                        newList,
                                        userNotification.getNotificationCampaignsCreateDate(),
                                        newList.isEmpty() ? 0L : userNotification.getLastSeenCampaign(),
                                        newList.isEmpty() ? 0L
                                                : deleteDto.getNotificationsByCreationDate().contains(userNotification.getLastSeenTransaction())
                                                ? newList.stream().map(Notification::getCreationDate).filter(n -> n < userNotification.getLastSeenTransaction()).findFirst().orElse(0L)
                                                : userNotification.getLastSeenTransaction(),
                                        newList.isEmpty() ? 0 : userNotification.getRemainNotificationCount(),
                                        newList.isEmpty() ? userNotification.getNotificationCampaignsCreateDate().size()
                                                : userNotification.getNotificationCount() - (userNotification.getNotificationTransactions().size() - newList.size())
                                ));

                    } else if (deleteDto.getNotificationType().equals(NoticeType.CAMPAIGN.getValue())) {
                        Flux<Long> notificationsIdFlux = deleteDto.getNotificationsByCreationDate().isEmpty() ?
                                Flux.empty() :
                                Flux.fromIterable(userNotification.getNotificationCampaignsCreateDate())
                                        .filter(notificationId -> !deleteDto.getNotificationsByCreationDate().contains(notificationId));

                        return notificationsIdFlux.sort((o1, o2) -> (int) (o2 - o1))
                                .collectList().map(newList -> new UserNotification(
                                        userNotification.getId(),
                                        userNotification.getSsn(),
                                        userNotification.getNotificationTransactions(),
                                        newList,
                                        newList.isEmpty() ? 0L
                                                : deleteDto.getNotificationsByCreationDate().contains(userNotification.getLastSeenCampaign())
                                                ? newList.stream().filter(n -> n < userNotification.getLastSeenCampaign()).findFirst().orElse(0L)
                                                : userNotification.getLastSeenCampaign(),
                                        newList.isEmpty() ? 0L : userNotification.getLastSeenTransaction(),
                                        newList.isEmpty() ? 0 : userNotification.getRemainNotificationCount(),
                                        newList.isEmpty() ? userNotification.getNotificationTransactions().size() : userNotification.getNotificationCount()
                                                - (userNotification.getNotificationCampaignsCreateDate().size() - newList.size())
                                ));

                    } else return Mono.error(new GeneralException("type.not.find", HttpStatus.NOT_FOUND));
                })
                .flatMap(this.userNotificationRepository::save);

    }




    //    @Override
//    public Mono<Void> clearUnreadCount(String ssn) {
//        return userNotificationRepository.findBySsn(ssn)
//                .switchIfEmpty(Mono.error(new GeneralException("ssn.not.find", HttpStatus.NOT_FOUND)))
//                .map(res -> new UserNotification(res.getId(),
//                        res.getSsn(),
//                        res.getNotifications(),
//                        res.getLastSeenNotificationId(),
//                        0,
//                        res.getNotificationCount()
//                ))
//                .flatMap(this.userNotificationRepository::save)
//                .then();
////                .map(user -> {
////                    UnreadNoticeCountResDto result = new UnreadNoticeCountResDto();
////                    result.setRemainNotificationCount(user.getRemainNotificationCount());
////                    result.setSsn(ssn);
////                    return result;
////                });
//    }


    //old UserLastSeenId method
//        return userNotificationRepository.findBySsn(ssn)
//                .switchIfEmpty(Mono.error(new GeneralException("ssn.not.find", HttpStatus.NOT_FOUND)))
//                .flatMap(res -> {
//                    Mono<Integer> lastSeenInxMono = Flux.fromIterable(res.getNotifications())
//                            .filter(notification -> notification.getId().equals(lastSeenId))
//                            .next()
//                            .map(notification -> res.getNotifications().indexOf(notification))
//                            .switchIfEmpty(Mono.error(new GeneralException("notification.id.not.valid", HttpStatus.NOT_FOUND)));
//
//                    return lastSeenInxMono.flatMap(lastSeenInx -> {
//                        if (res.getLastSeenNotificationIndex() > lastSeenInx)
//                            return Mono.error(new GeneralException("pre.id.is.newer", HttpStatus.BAD_REQUEST));
//
//                        return Mono.just(new UserNotification(res.getId(),
//                                res.getSsn(),
//                                res.getNotifications(),
//                                lastSeenId,
//                                lastSeenInx,
//                                !res.getLastSeenNotificationId().equals("") ? res.getLastSeenNotificationIndex() : res.getPreviousNotificationIndex(),
//                                Math.abs(res.getRemainNotificationCount() -
//                                        (lastSeenInx - (res.getLastSeenNotificationIndex() == 0 ? res.getPreviousNotificationIndex() : res.getLastSeenNotificationIndex()))
//                                )));
//
//                    });
//                })
//                .flatMap(this.userNotificationRepository::save)
//                .map(user -> {
//                    UserNoticeListResDto result = new UserNoticeListResDto();
//                    result.setLastSeenId(user.getLastSeenNotificationId());
//                    result.setSsn(ssn);
//                    return result;
//                });
}
