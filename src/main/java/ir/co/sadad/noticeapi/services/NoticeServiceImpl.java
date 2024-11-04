package ir.co.sadad.noticeapi.services;

import ir.co.sadad.noticeapi.dtos.DeleteNoticeReqDto;
import ir.co.sadad.noticeapi.dtos.LastSeenResDto;
import ir.co.sadad.noticeapi.dtos.UnreadNoticeCountResDto;
import ir.co.sadad.noticeapi.dtos.UserNoticeListResDto;
import ir.co.sadad.noticeapi.enums.NoticeType;
import ir.co.sadad.noticeapi.enums.Platform;
import ir.co.sadad.noticeapi.exceptions.GeneralException;
import ir.co.sadad.noticeapi.models.Notification;
import ir.co.sadad.noticeapi.models.UserNotification;
import ir.co.sadad.noticeapi.repositories.NotificationRepository;
import ir.co.sadad.noticeapi.repositories.UserNotificationRepository;
import ir.co.sadad.noticeapi.services.utilities.Utilities;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Slf4j
@RequiredArgsConstructor
@Service
public class NoticeServiceImpl implements NoticeService {

    private final NotificationRepository notificationRepository;

    private final UserNotificationRepository userNotificationRepository;

    private final ReactiveMongoTemplate reactiveMongoTemplate;

    @Value("${notifications.page-size}")
    private int pageSize;

    @Override
    public Mono<UserNoticeListResDto> userNoticeList(String ssn, String type, int page, String userAgent) {
        Platform userCurrentPlatform = Utilities.checkUserAgent(userAgent);

        UserNoticeListResDto res = new UserNoticeListResDto();

        return userNotificationRepository.findBySsn(ssn)
//                .switchIfEmpty(Mono.defer(() -> pickGeneralNoticesForNewMember(ssn)))
//                .flatMap(this::pickGeneralNotices)
                .flatMap(userNotification -> {
                    Flux<Notification> allNotificationsFlux;

                    if (type.equals(NoticeType.TRANSACTION.getValue())) {
                        allNotificationsFlux = Flux.fromIterable(userNotification.getNotificationTransactions() == null ? new ArrayList<>()
                                : userNotification.getNotificationTransactions());

                    } else if (type.equals(NoticeType.CAMPAIGN.getValue())) {
                        allNotificationsFlux = filteredUserNotifications(userNotification, userCurrentPlatform);

                    } else {
                        return Mono.error(new GeneralException("type.not.find", HttpStatus.BAD_REQUEST));
                    }

                    return allNotificationsFlux
//                            .switchIfEmpty(Flux.empty())
                            .sort((o1, o2) -> (o2.getId().compareTo(o1.getId())))
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

                                int startIndex = (page - 1) * pageSize;
                                int endIndex = Math.min(startIndex + pageSize, totalItems);

                                startIndex = Math.max(startIndex, 0);  // Ensure startIndex is not negative
                                endIndex = Math.min(endIndex, totalItems);  // Ensure endIndex does not exceed the list size

                                res.setCurrentPage(page);
                                res.setTotalPages(totalPages);
                                res.setNotifications(notifications.isEmpty() ? Collections.emptyList() : notifications.subList(startIndex, endIndex));

                                if (!notifications.isEmpty())
                                    return userLastSeenId(ssn, notifications.get(0).getCreationDate(), null)
                                            .map(LastSeenResDto::getLastSeenCampaign)
                                            .flatMap(aLong -> {
                                                res.setLastSeenCampaign(aLong);
                                                return Mono.just(res);
                                            });
                                else res.setLastSeenCampaign(userNotification.getLastSeenCampaign());

                                res.setLastSeenTransaction(userNotification.getLastSeenTransaction());

                                return Mono.just(res);
                            });
                })
                .switchIfEmpty(Mono.defer(() -> {
                    res.setCurrentPage(page);
                    res.setNotifications(Collections.emptyList());
                    return Mono.just(res);
                }));
    }

    private Flux<Notification> filteredUserNotifications(UserNotification userNotice, Platform userCurrentPlatform) {
        Flux<Long> notificationsIdFlux = Flux.concat(
                Flux.fromIterable(userNotice.getNotificationCampaignsCreateDate() == null ? new ArrayList<>() : userNotice.getNotificationCampaignsCreateDate()),
                Flux.fromIterable(userNotice.getNotificationGeneralCreateDate() == null ? new ArrayList<>() : userNotice.getNotificationGeneralCreateDate()));
        return notificationsIdFlux
                //processing each notificationId in parallel
                .flatMapSequential(notificationId -> reactiveMongoTemplate.findOne(notificationRepository.
                        findByCreationDateAndPlatformAndStatusIsAndActivationDate(notificationId, userCurrentPlatform,
                                Utilities.getCurrentUTCDate().concat("T20:30:00.000Z")), Notification.class)
                )
                .switchIfEmpty(Mono.empty())
                .filter(Objects::nonNull); // Filter out empty values
    }

    private Mono<? extends UserNotification> pickGeneralNoticesForNewMember(String ssn) {
        return notificationRepository.findByType(NoticeType.GENERAL.getValue())
                .switchIfEmpty(Mono.defer(Mono::empty))
                .map(Notification::getCreationDate)
                .collectList()
                .flatMap(generalNoticesId -> {
                    if (generalNoticesId.isEmpty()) {
                        return Mono.empty();
                    } else {
                        return saveGeneralForUser(ssn, generalNoticesId, true);
                    }
                });
    }

    private Mono<? extends UserNotification> pickGeneralNotices(UserNotification userNotification) {
        return notificationRepository.findByType(NoticeType.GENERAL.getValue())
                .switchIfEmpty(Mono.defer(Mono::empty))
                .collectList()
                .flatMap(allGeneralNotices -> {
                            if (userNotification.getNotificationGeneralCreateDate() != null
                                    && !userNotification.getNotificationGeneralCreateDate().isEmpty())
                                return Flux.fromIterable(userNotification.getNotificationGeneralCreateDate())
                                        .sort()
                                        .last()
                                        .flatMap(greatestUserGeneralNotice -> {
                                            List<Long> newGeneralNotices = allGeneralNotices.stream()
                                                    .map(Notification::getCreationDate)
                                                    .filter(creationDate -> creationDate.compareTo(greatestUserGeneralNotice) > 0)
                                                    .toList();

                                            if (!newGeneralNotices.isEmpty()) {
                                                return saveGeneralForUser(userNotification.getSsn(), newGeneralNotices, false);
                                            }
                                            return Mono.just(userNotification);
                                        });
                            else
                                // if user does not have any general notification by now
                                return saveGeneralForUser(userNotification.getSsn(), allGeneralNotices.stream()
                                        .map(Notification::getCreationDate).toList(), false);
                        }
                );

    }

    private Mono<UserNotification> saveGeneralForUser(String ssn, List<Long> newGeneralNotices, boolean newMember) {
        if (!newMember)
            return userNotificationRepository.findBySsn(ssn)
                    .flatMap(userNotice -> {
                        userNotice.setNotificationGeneralCreateDate(Stream
                                .concat(userNotice.getNotificationGeneralCreateDate() != null && !userNotice.getNotificationGeneralCreateDate().isEmpty()
                                        ? userNotice.getNotificationGeneralCreateDate().stream()
                                        : Stream.empty(), newGeneralNotices.stream()).toList());
                        userNotice.setNotificationCount(userNotice.getNotificationCount() + newGeneralNotices.size());
                        return userNotificationRepository.save(userNotice);

                    })
                    .onErrorMap(throwable -> new GeneralException(throwable.getMessage(), "error.on.save.user.notification"));

        else
            return userNotificationRepository.insert(UserNotification
                            .builder()
                            .ssn(ssn)
                            .notificationGeneralCreateDate(newGeneralNotices)
                            .lastSeenCampaign(0L)
                            .lastSeenTransaction(0L)
                            .remainNotificationCount(newGeneralNotices.size())
                            .notificationCount(newGeneralNotices.size())
                            .build())
                    .onErrorMap(throwable -> new GeneralException(throwable.getMessage(), "error.on.save.user.notification"));

    }

    @Override
    public Mono<LastSeenResDto> userLastSeenId(String ssn, Long lastSeenCampaign, Long lastSeenTransaction) {

        return userNotificationRepository.findBySsn(ssn)
                .switchIfEmpty(Mono.error(new GeneralException("ssn.not.find", HttpStatus.NOT_FOUND)))
                .flatMap(res -> Mono.just(new UserNotification(res.getId(),
                        res.getSsn(),
                        res.getNotificationTransactions(),
                        res.getNotificationCampaignsCreateDate(),
                        res.getNotificationGeneralCreateDate(),
                        lastSeenCampaign != null ? lastSeenCampaign : res.getLastSeenCampaign(),
                        lastSeenTransaction != null ? lastSeenTransaction : res.getLastSeenTransaction(),
                        res.getRemainNotificationCount(),
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
    public Mono<UnreadNoticeCountResDto> unreadNoticeCount(String ssn, String userAgent) {
        Platform userCurrentPlatform = Utilities.checkUserAgent(userAgent);

        UnreadNoticeCountResDto res = new UnreadNoticeCountResDto();

        return userNotificationRepository.findBySsn(ssn)
                .switchIfEmpty(Mono.defer(() -> pickGeneralNoticesForNewMember(ssn)))
                .flatMap(this::pickGeneralNotices)
                .flatMap(userNotification -> {
                    Flux<Notification> allNotificationsFlux = filteredUserNotifications(userNotification, userCurrentPlatform);

                    return allNotificationsFlux
                            .map(Notification::getCreationDate)
                            .filter(date -> date > userNotification.getLastSeenCampaign())
                            .count()
                            .map(Long::intValue)
                            .map(count -> {
                                res.setRemainNotificationCount(count);
                                res.setLastSeenCampaign(userNotification.getLastSeenCampaign());
                                res.setLastSeenTransaction(userNotification.getLastSeenTransaction());
                                return res;
                            });

                })
                .switchIfEmpty(Mono.defer(() -> {
                    res.setRemainNotificationCount(0);
                    return Mono.just(res);
                }));
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
                                        userNotification.getNotificationGeneralCreateDate(),
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
                        Mono<List<Long>> generalNotificationsIdRemained =
                                Mono.justOrEmpty(userNotification.getNotificationGeneralCreateDate())
                                        .map(notificationDates -> notificationDates.stream()
                                                .filter(notificationId -> !deleteDto.getNotificationsByCreationDate().contains(notificationId))
                                                .toList())
                                        .defaultIfEmpty(Collections.emptyList());

                        Flux<Long> campaignNotificationsIdFluxRemained =
                                Flux.fromIterable(userNotification.getNotificationCampaignsCreateDate() != null && !userNotification.getNotificationCampaignsCreateDate().isEmpty()
                                                ? userNotification.getNotificationCampaignsCreateDate() : new ArrayList<>())
                                        .filter(notificationId -> !deleteDto.getNotificationsByCreationDate().contains(notificationId));

                        return campaignNotificationsIdFluxRemained
                                .sort((o1, o2) -> (int) (o2 - o1))
                                .collectList()
                                .zipWith(generalNotificationsIdRemained, (newCampaignList, newGeneralList) ->
                                        new UserNotification(
                                                userNotification.getId(),
                                                userNotification.getSsn(),
                                                userNotification.getNotificationTransactions(),
                                                newCampaignList,
                                                newGeneralList,
                                                userNotification.getLastSeenCampaign(),
                                                userNotification.getLastSeenTransaction(),
                                                newCampaignList.isEmpty() ? 0 : userNotification.getRemainNotificationCount(),
                                                userNotification.getNotificationTransactions() != null && !userNotification.getNotificationTransactions().isEmpty()
                                                        ? userNotification.getNotificationTransactions().size() + newCampaignList.size() + newGeneralList.size()
                                                        : newCampaignList.size() + newGeneralList.size()
                                        ));

                    } else return Mono.error(new GeneralException("type.not.find", HttpStatus.NOT_FOUND));
                })
                .flatMap(this.userNotificationRepository::save);

    }

//    // it calculates correctly but takes notificationId from list, regardless of notification type in terms of activationDate and platform
//    private Long calculateLastSeenIdAfterDelete(DeleteNoticeReqDto deleteDto, UserNotification userNotif, List<Long> newCampaignList, List<Long> newGeneralList) {
//        if (newGeneralList.isEmpty() && newCampaignList.isEmpty()) return 0L;
//        else return deleteDto.getNotificationsByCreationDate().contains(userNotif.getLastSeenCampaign())
//                ? newCampaignList.stream().filter(n -> n < userNotif.getLastSeenCampaign()).max(Comparator.naturalOrder()).orElse(
//                newGeneralList.stream().filter(n -> n < userNotif.getLastSeenCampaign()).max(Comparator.naturalOrder()).orElse(0L))
//                : userNotif.getLastSeenCampaign();
//    }


}
