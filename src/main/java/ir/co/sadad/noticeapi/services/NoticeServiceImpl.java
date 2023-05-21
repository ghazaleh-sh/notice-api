package ir.co.sadad.noticeapi.services;

import ir.co.sadad.noticeapi.dtos.*;
import ir.co.sadad.noticeapi.enums.NoticeType;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

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

    //    @Value("${notifications.job.max-count}")
    private int MAXIMUM_NOTIFICATIONS = 2;

    //just sample - we should use kafka
    @Override
    public Mono<SendSingleNoticeResDto> sendSingleNotice(SendSingleNoticeReqDto singleNoticeReqDto) {
        SendSingleNoticeResDto res = new SendSingleNoticeResDto();

        List<Notification> notice = new ArrayList<>();
        notice.add(Notification.builder()
                .creationDate(System.currentTimeMillis())
                .account(singleNoticeReqDto.getAccount())
                .balance(singleNoticeReqDto.getBalance())
                .withdraw(singleNoticeReqDto.getWithdraw())
                .date(singleNoticeReqDto.getDate())
                .bankName(singleNoticeReqDto.getBankName())
                .type(NoticeType.TRANSACTION.getValue())
                .build());

        return Mono
                .just(singleNoticeReqDto)
                .flatMap(currentDto ->
                        userNotificationRepository.findBySsn(currentDto.getSsn())
                                .flatMap(userNotif -> {
                                    List<Notification> notifsOfUser = userNotif != null ? userNotif.getNotificationTransactions() : new ArrayList<>();
                                    if (notifsOfUser == null) notifsOfUser = new ArrayList<>();
                                    notifsOfUser.add(notice.get(0));
                                    assert userNotif != null;
                                    userNotif.setNotificationTransactions(notifsOfUser);
                                    userNotif.setRemainNotificationCount(userNotif.getRemainNotificationCount() + 1);
                                    userNotif.setNotificationCount(userNotif.getNotificationCount() + 1);
                                    success.getAndIncrement();
                                    return userNotificationRepository.save(userNotif);

                                })
                                .switchIfEmpty(Mono.defer(() -> userNotificationRepository.insert(UserNotification
                                        .builder()
                                        .ssn(currentDto.getSsn())
                                        .notificationTransactions(notice)
                                        .notificationCampaignsCreateDate(null)
                                        .lastSeenCampaign(-1L)
                                        .lastSeenTransaction(-1L)
                                        .remainNotificationCount(1)
                                        .notificationCount(1)
                                        .build())))

                                .onErrorMap(throwable -> new ValidationException(throwable.getMessage(), "error.on.save.user.notification"))

                                .flatMap(notif -> {
                                    res.setId(notif.getId());
                                    res.setSsn(singleNoticeReqDto.getSsn());
                                    res.setStatus("success");
                                    return Mono.just(res);
                                }));
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
                .flatMap(camp -> notificationRepository.findByDateAndType(camp.getDate(), "2"))
                .cache()
                .switchIfEmpty(Mono.defer(() -> notificationRepository.insert(Notification
                        .builder()
                        .creationDate(System.currentTimeMillis())
                        .description(campaignNoticeReqDto.getDescription())
                        .title(campaignNoticeReqDto.getTitle())
                        .date(campaignNoticeReqDto.getDate())
                        .type(NoticeType.CAMPAIGN.getValue())
                        .build())))
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
                .lastSeenCampaign(-1L)
                .lastSeenTransaction(-1L)
                .remainNotificationCount(1)
                .notificationCount(1)
//                .lastSeenNotificationIndex(0)
//                .previousNotificationIndex(-1)
                .build())
                .onErrorMap(throwable -> new GeneralException(throwable.getMessage(), "error.on.save.user.notification"));
        // just to see what is being emitted
//                .log();
    }

    @Override
    public Mono<UserNoticeListResDto> userNoticeList(String ssn, String type, int page) {

        return userNotificationRepository.findBySsn(ssn)
                .switchIfEmpty(Mono.error(new GeneralException("ssn.not.find", HttpStatus.NOT_FOUND)))
                .flatMap(userNotification -> {
                    // Atomic to prevent race condition,
                    AtomicLong totalItems = new AtomicLong(0);
                    AtomicInteger totalPages = new AtomicInteger(0);

                    AtomicInteger startIndex = new AtomicInteger(0);
                    AtomicInteger endIndex = new AtomicInteger(0);

                    Flux<Notification> notificationsFlux = null;

                    if (type.equals(NoticeType.TRANSACTION.getValue())) {
                        notificationsFlux = Flux.fromIterable(userNotification.getNotificationTransactions());

                        notificationsFlux.count().subscribe(count -> {
                            totalItems.set(count);
                            // ceil method to round up the argument to the nearest integer value
                            totalPages.set((int) Math.ceil((double) totalItems.get() / pageSize));
                        });
                    }


                    if (type.equals(NoticeType.CAMPAIGN.getValue())) {
                        Flux<Long> notificationsIdFlux = Flux.fromIterable(userNotification.getNotificationCampaignsCreateDate());

                        notificationsIdFlux.count().subscribe(count -> {
                            totalItems.set(count);
                            // ceil method to round up the argument to the nearest integer value
                            totalPages.set((int) Math.ceil((double) totalItems.get() / pageSize));
                        });

                        notificationsFlux = notificationsIdFlux.flatMap(notificationRepository::findByCreationDate)
                                .collectList()
                                .flatMapMany(Flux::fromIterable);
                    }


                    if (totalPages.get() != 0 && page > totalPages.get())
                        return Mono.error(new GeneralException("page.is.more.than.total", HttpStatus.BAD_REQUEST));

                    startIndex.set((page - 1) * pageSize);
                    endIndex.set((int) Math.min(startIndex.get() + pageSize, totalItems.get()));

                    /*
                    for concerning about backpressure when collecting the list of notifications,
                    you can use the Flux.fromIterable method to convert the list to a Flux
                    and then apply backpressure operators on it.
                    */
                    return notificationsFlux
//                            .switchIfEmpty(Flux.empty())
                            .sort((o1, o2) -> (int) (o2.getCreationDate() - o1.getCreationDate()))
                            .onBackpressureBuffer(20) // buffer up to 20 items
                            .limitRate(20) // limit the rate at which items are emitted to 20
                            .collectList()
                            .flatMap(notifications -> {
                                UserNoticeListResDto res = new UserNoticeListResDto();
                                res.setSsn(ssn);
                                res.setCurrentPage(page);
                                res.setTotalPages(totalPages.get());
                                res.setNotifications(notifications.isEmpty() ? Collections.emptyList() : notifications.subList(startIndex.get(), endIndex.get()));
                                res.setLastSeenCampaign(userNotification.getLastSeenCampaign());
                                res.setLastSeenTransaction(userNotification.getLastSeenTransaction());

                                return Mono.just(res);
                            });

                });

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
    @Override
    public Mono<LastSeenResDto> UserLastSeenId(String ssn, Long lastSeenCampaign, Long lastSeenTransaction) {

        return userNotificationRepository.findBySsn(ssn)
                .switchIfEmpty(Mono.error(new GeneralException("ssn.not.find", HttpStatus.NOT_FOUND)))
                .flatMap(res -> Mono.just(new UserNotification(res.getId(),
                        res.getSsn(),
                        res.getNotificationTransactions(),
                        res.getNotificationCampaignsCreateDate(),
                        lastSeenCampaign,
                        lastSeenTransaction,
                        0,//res.getRemainNotificationCount(),
                        res.getNotificationCount()
                )))
                .flatMap(this.userNotificationRepository::save)
                .map(user -> {
                    LastSeenResDto result = new LastSeenResDto();
                    result.setLastSeenCampaign(user.getLastSeenCampaign());
                    result.setLastSeenTransaction(user.getLastSeenTransaction());
                    result.setSsn(ssn);
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
                    res.setSsn(ssn);
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
                                        newList.isEmpty() ? -1L : userNotification.getLastSeenCampaign(),
                                        newList.isEmpty() ? -1L
                                                : deleteDto.getNotificationsByCreationDate().contains(userNotification.getLastSeenTransaction())
                                                ? newList.stream().map(Notification::getCreationDate).filter(n -> n < userNotification.getLastSeenTransaction()).findFirst().orElse(-1L)
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
                                        newList.isEmpty() ? -1L
                                                : deleteDto.getNotificationsByCreationDate().contains(userNotification.getLastSeenCampaign())
                                                ? newList.stream().filter(n -> n < userNotification.getLastSeenCampaign()).findFirst().orElse(-1L)
                                                : userNotification.getLastSeenCampaign(),
                                        newList.isEmpty() ? -1L : userNotification.getLastSeenTransaction(),
                                        newList.isEmpty() ? 0 : userNotification.getRemainNotificationCount(),
                                        newList.isEmpty() ? userNotification.getNotificationTransactions().size() : userNotification.getNotificationCount()
                                                - (userNotification.getNotificationCampaignsCreateDate().size() - newList.size())
                                ));

                    } else return Mono.error(new GeneralException("type.not.find", HttpStatus.NOT_FOUND));
                })
                .flatMap(this.userNotificationRepository::save);

    }

//    private Integer setPreviousIndex(Integer preInx, int size) {
//        if ((preInx - size + 1) > -2)
//            return preInx - size + 1;
//        else
//            return -1;
//
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


//    @Override // this service is called by another application Rest Api at midnight
//    public void resetNoticeCountJob() {
//
//        userNotificationRepository.findAll()
//                .filter(userNotification -> userNotification.getNotificationCount() > MAXIMUM_NOTIFICATIONS)
//                .flatMap(this::deleteMoreThan5Hundred)
//                .log()
//                .subscribe(); //to execute the code inside the flatMap() operator.
//
//    }
//
//    private Mono<UserNotification> deleteMoreThan5Hundred(UserNotification user) {
//        int deleteCount = user.getNotificationCount() - MAXIMUM_NOTIFICATIONS;
//
//        user.getNotifications().sort((o1, o2) -> (int) (o1.getCreationDate() - o2.getCreationDate()));
//
//        List<Notification> deleteNotifs = user.getNotifications().subList(0, deleteCount);
//
//        Flux<Notification> filteredNotifications = Flux.fromIterable(user.getNotifications())
//                .filter(notification -> !deleteNotifs.contains(notification));
//
//        return filteredNotifications
//                .collectList()
//                .map(newList -> new UserNotification(
//                        user.getId(),
//                        user.getSsn(),
//                        newList,
//                        user.getLastSeenNotificationId(),
//                        user.getRemainNotificationCount() > MAXIMUM_NOTIFICATIONS ? MAXIMUM_NOTIFICATIONS : user.getRemainNotificationCount(),
//                        MAXIMUM_NOTIFICATIONS
//                ))
//                .flatMap(this.userNotificationRepository::save);
//    }
}
