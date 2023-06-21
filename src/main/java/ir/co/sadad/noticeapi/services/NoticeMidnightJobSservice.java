package ir.co.sadad.noticeapi.services;

import ir.co.sadad.noticeapi.models.Notification;
import ir.co.sadad.noticeapi.models.UserNotification;
import ir.co.sadad.noticeapi.repositories.NotificationRepository;
import ir.co.sadad.noticeapi.repositories.UserNotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class NoticeMidnightJobSservice {

    private final NotificationRepository notificationRepository;

    private final UserNotificationRepository userNotificationRepository;

    //    @Value("${notifications.job.max-count}")
    private int MAXIMUM_NOTIFICATIONS = 3;


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
        int ramainDelete = deleteCount - user.getNotificationTransactions().size();

        if (ramainDelete <= 0) { // means transaction notices are more than(or equal) max, so we can delete whatever are needed
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
                            user.getLastSeenCampaign(),
                            transactionsCandidForDelete.stream().anyMatch(noticDelete -> noticDelete.getCreationDate().equals(user.getLastSeenTransaction())) ? 0L : user.getLastSeenTransaction(),
                            user.getRemainNotificationCount(),
                            MAXIMUM_NOTIFICATIONS
                    ))
                    .flatMap(this.userNotificationRepository::save)
                    .log();
        } else { // means we deleted all transaction and needed to delete more(=remainDelete) from campaign

            Collections.sort(user.getNotificationCampaignsCreateDate());

            List<Long> campaignsCandidForDelete = user.getNotificationCampaignsCreateDate().subList(0, ramainDelete);
            Flux<Long> filteredNotifications = Flux.fromIterable(user.getNotificationCampaignsCreateDate())
                    .filter(campaignCreateDate -> !campaignsCandidForDelete.contains(campaignCreateDate));

            return filteredNotifications
                    .collectList()
                    .map(newList -> new UserNotification(
                            user.getId(),
                            user.getSsn(),
                            new ArrayList<Notification>(),
                            newList,
                            campaignsCandidForDelete.stream().anyMatch(noticDelete -> noticDelete.equals(user.getLastSeenCampaign())) ? 0L : user.getLastSeenCampaign(),
                            0L,
                            user.getRemainNotificationCount(),
                            MAXIMUM_NOTIFICATIONS
                    ))
                    .flatMap(this.userNotificationRepository::save)
                    .log();
        }
    }

    private Mono<UserNotification> deleteMoreThanMonth(UserNotification user) {
        long oneMonthAgoMillis = DateTime.now().minusMonths(1).getMillis();

        List<Long> campaignsCandidForDelete = user.getNotificationCampaignsCreateDate().stream()
                .filter(campaignCreateDate -> campaignCreateDate.compareTo(oneMonthAgoMillis) < 0).collect(Collectors.toList());

        List<Notification> transactionsCandidForDelete = user.getNotificationTransactions().stream()
                .filter(transactionNotice -> transactionNotice.getCreationDate().compareTo(oneMonthAgoMillis) < 0).collect(Collectors.toList());

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
                        campaignsCandidForDelete.stream().anyMatch(noticDelete -> noticDelete.equals(user.getLastSeenCampaign())) ? 0L : user.getLastSeenCampaign(),
                        transactionsCandidForDelete.stream().anyMatch(noticDelete -> noticDelete.getCreationDate().equals(user.getLastSeenTransaction())) ? 0L : user.getLastSeenTransaction(),
                        user.getRemainNotificationCount(),
                        user.getNotificationCount() - campaignsCandidForDelete.size() - transactionsCandidForDelete.size()
                ))
                .flatMap(this.userNotificationRepository::save)
                .log();

    }

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

}
