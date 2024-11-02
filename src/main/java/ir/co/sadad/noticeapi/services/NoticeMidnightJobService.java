package ir.co.sadad.noticeapi.services;

import ir.co.sadad.noticeapi.dtos.PushNotificationReqDto;
import ir.co.sadad.noticeapi.enums.NoticeType;
import ir.co.sadad.noticeapi.models.Notification;
import ir.co.sadad.noticeapi.models.UserNotification;
import ir.co.sadad.noticeapi.repositories.NotificationRepository;
import ir.co.sadad.noticeapi.repositories.UserNotificationRepository;
import ir.co.sadad.noticeapi.services.utilities.Utilities;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class NoticeMidnightJobService {

    private final NotificationRepository notificationRepository;

    private final UserNotificationRepository userNotificationRepository;

    //    @Value("${notifications.job.max-count}")
    private final int MAXIMUM_NOTIFICATIONS = 5;

    private final ModelMapper modelMapper;

    private final PushNotificationProviderService pushNotificationService;

    // this service is called by job Rest Api day by day at 5 o'clock
    public void pushNotificationBasedOnActivationDate() {
        notificationRepository.findByPushNotificationIsTrueAndActivationDate(Utilities.getCurrentUTCDate().concat("T20:30:00.000Z"))
                .switchIfEmpty(Mono.defer(Mono::empty))
                .flatMap(savedNotice -> {
                    PushNotificationReqDto pushReqDto = new PushNotificationReqDto();
                    modelMapper.map(savedNotice, pushReqDto);
                    if (NoticeType.GENERAL.getValue().equals(savedNotice.getType()))
                        pushReqDto.setSuccessSsn(Collections.emptyList());
//          TODO:          else{
//                       I might save ssn list in this condition and here just gt from the document
//                    }
                    return pushNotificationService.multiCastPushNotification(pushReqDto);
                });
    }

    // this service is called by notice-api-job Rest Api at midnight
    public void resetNoticeCountJob() {

        userNotificationRepository.findAll()
                .filter(userNotification -> userNotification.getNotificationCount() > MAXIMUM_NOTIFICATIONS)
//                .flatMap(this::deleteMoreThanMax)
//                .flatMap(this::deleteMoreThanMonth)
                .flatMap(this::deleteCampaignDeletedMessageFromUsers)
                .log()
                .subscribe(); //to execute the code inside the flatMap() operator.

    }

    private Mono<UserNotification> deleteMoreThanMax(UserNotification user) {
        int deleteCount = user.getNotificationCount() - MAXIMUM_NOTIFICATIONS;
        int remainDelete = deleteCount - (user.getNotificationTransactions() != null && !user.getNotificationTransactions().isEmpty()
                ? user.getNotificationTransactions().size() : 0);

        if (remainDelete <= 0) { // means transaction notices are more than(or equal) max, so we can delete whatever are needed
            user.getNotificationTransactions().sort((o1, o2) -> (int) (o1.getCreationDate() - o2.getCreationDate()));

            List<Notification> transactionsCandidForDelete = user.getNotificationTransactions().subList(0, deleteCount);

            Flux<Notification> filteredNotifications = Flux.fromIterable(user.getNotificationTransactions())
                    .filter(notification -> !transactionsCandidForDelete.contains(notification));

            return filteredNotifications
                    .collectList()
                    .map(newList -> new UserNotification(
                            user.getId(),
                            user.getSsn(),
                            newList,
                            user.getNotificationCampaignsCreateDate(),
                            user.getNotificationGeneralCreateDate(),
                            user.getLastSeenCampaign(),
                            transactionsCandidForDelete.stream().anyMatch(noticDelete -> noticDelete.getCreationDate().equals(user.getLastSeenTransaction())) ? 0L : user.getLastSeenTransaction(),
                            user.getRemainNotificationCount(),
                            MAXIMUM_NOTIFICATIONS
                    ))
                    .flatMap(this.userNotificationRepository::save)
                    .log();
        } else { // means we deleted all transaction and needed to delete more(=remainDelete) from campaign

            Mono<List<Long>> filteredNotifications = findCampaignsCandidForDelete(user, remainDelete)
                    .flatMap(candidForDelete -> {
                        List<Long> updatedCampaigns = user.getNotificationCampaignsCreateDate()
                                .stream()
                                .filter(campaignCreateDate -> !candidForDelete.contains(campaignCreateDate))
                                .toList();

                        return Mono.just(updatedCampaigns);
                    });


            return filteredNotifications
                    .map(newCampaignList -> new UserNotification(
                            user.getId(),
                            user.getSsn(),
                            new ArrayList<>(),
                            newCampaignList,
                            new ArrayList<>(),
                            !newCampaignList.contains(user.getLastSeenCampaign()) ? 0L : user.getLastSeenCampaign(),
                            0L,
                            newCampaignList.size(),
                            newCampaignList.size()
                    ))
                    .flatMap(this.userNotificationRepository::save)
                    .log();
        }
    }

    private Mono<List<Long>> findCampaignsCandidForDelete(UserNotification userNotification, int remainDelete) {
        return Flux.fromIterable(userNotification.getNotificationCampaignsCreateDate())
                .flatMap(campDate -> notificationRepository.findByCreationDate(campDate)
                        .filter(notification -> notification.getActivationDate() == null
                                || notification.getActivationDate().split("T")[0].compareTo(Utilities.getCurrentUTCDate()) < 0)
                        .map(Notification::getCreationDate)
                )
                .take(remainDelete)
                .collectList();
    }

    private Mono<UserNotification> deleteMoreThanMonth(UserNotification user) {
        long oneMonthAgoMillis = DateTime.now().minusMonths(1).getMillis();

        List<Long> campaignsCandidForDelete = user.getNotificationCampaignsCreateDate().stream()
                .filter(campaignCreateDate -> campaignCreateDate.compareTo(oneMonthAgoMillis) < 0).toList();

        List<Notification> transactionsCandidForDelete = user.getNotificationTransactions().stream()
                .filter(transactionNotice -> transactionNotice.getCreationDate().compareTo(oneMonthAgoMillis) < 0).toList();

        Flux<Notification> filteredNotifications = Flux.fromIterable(user.getNotificationTransactions())
                .filter(notification -> !transactionsCandidForDelete.contains(notification));

        return filteredNotifications
                .collectList()
                .map(newList -> new UserNotification(
                        user.getId(),
                        user.getSsn(),
                        newList,
                        user.getNotificationCampaignsCreateDate().stream()
                                .filter(campaignCreateDate -> !campaignsCandidForDelete.contains(campaignCreateDate)).collect(Collectors.toList()),
                        user.getNotificationGeneralCreateDate(),
                        campaignsCandidForDelete.stream().anyMatch(noticDelete -> noticDelete.equals(user.getLastSeenCampaign())) ? 0L : user.getLastSeenCampaign(),
                        transactionsCandidForDelete.stream().anyMatch(noticDelete -> noticDelete.getCreationDate().equals(user.getLastSeenTransaction())) ? 0L : user.getLastSeenTransaction(),
                        user.getRemainNotificationCount(),
                        user.getNotificationCount() - campaignsCandidForDelete.size() - transactionsCandidForDelete.size()
                ))
                .flatMap(this.userNotificationRepository::save)
                .log();

    }

    //TODO: amount of general notifications of each user should be considered as well.
    private Mono<UserNotification> deleteCampaignDeletedMessageFromUsers(UserNotification user) {

        Flux<Long> filteredNotifications = Flux.fromIterable(user.getNotificationCampaignsCreateDate())
                .flatMap(campaignCreateDate -> notificationRepository.findByCreationDate(campaignCreateDate)
                        .switchIfEmpty(Mono.empty()))
//                .filter(Objects::nonNull)
                .mapNotNull(Notification::getCreationDate)
                .collectList()
                .flatMapMany(Flux::fromIterable);

        Mono<Long> notificationCountMono = filteredNotifications.count();
        Mono<UserNotification> updatedUserNotificationMono = Mono.zip(
                Mono.just(user.getNotificationCount()),
                Mono.just(user.getNotificationCampaignsCreateDate().size()),
                notificationCountMono
        ).flatMap(tuple -> {
            int updatedNotificationCount = tuple.getT1() - (tuple.getT2() - tuple.getT3().intValue());
            return filteredNotifications
                    .collectList()
                    .map(notificationList -> new UserNotification(
                            user.getId(),
                            user.getSsn(),
                            user.getNotificationTransactions(),
                            notificationList,
                            user.getNotificationGeneralCreateDate(),
                            notificationList.stream().anyMatch(noticDelete -> noticDelete.equals(user.getLastSeenCampaign())) ? user.getLastSeenCampaign() : 0L,
                            user.getLastSeenTransaction(),
                            user.getRemainNotificationCount(),
                            updatedNotificationCount
                    ));
        });

        return updatedUserNotificationMono
                .flatMap(this.userNotificationRepository::save)
                .log();

    }

    //TODO:  create a job to removeDeletedNotificationsOfUserList()

}
