package ir.co.sadad.noticeapi.services;

import ir.co.sadad.noticeapi.dtos.DeleteNoticeReqDto;
import ir.co.sadad.noticeapi.dtos.LastSeenResDto;
import ir.co.sadad.noticeapi.dtos.UnreadNoticeCountResDto;
import ir.co.sadad.noticeapi.dtos.UserNoticeListResDto;
import ir.co.sadad.noticeapi.models.Notification;
import ir.co.sadad.noticeapi.models.UserNotification;
import ir.co.sadad.noticeapi.repositories.NotificationRepository;
import ir.co.sadad.noticeapi.repositories.UserNotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NoticeServiceImplTest {

    @Mock
    private UserNotificationRepository userNotificationRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private ReactiveMongoTemplate reactiveMongoTemplate;

    //given
    private static final String SSN = "123456789";
    private static final String TYPE = "campaign";
    private static final int PAGE = 1;

    private UserNotification createUserNotification() {
        UserNotification userNotification = new UserNotification("1", SSN, null, null,null, 12345L, 12346L, 0, 2);
        userNotification.setNotificationTransactions(Arrays.asList(
                new Notification("1", "1105-14:00", 12346L, null, null, "transaction", "5000",
                        "34567677", "1000", "lgd", "moneyTransfer", null,null, null, null, null,null, null, null, null, null)
        ));

        return userNotification;
    }

    @Test
    void sendCampaignNotice() {
    }

    @Test
    void shouldReturnUserNotificationListForGivenSSNAndTypeAndPage() {

        Notification noticCamp = null; //new Notification("2", null, 23456L, "reminder", "descriptionnnn", "campaign", null, null, null, null,null, null, null, null, null);
        Notification noticCamp3 = null;//new Notification("3", null, 76767L, "reminder2", "descriptionnnn2", "campaign", null, null, null, null,null, null,null, null, null);
        when(notificationRepository.findByCreationDate(23456L)).thenReturn(Mono.just(noticCamp));
        when(notificationRepository.findByCreationDate(76767L)).thenReturn(Mono.just(noticCamp3));

        UserNotification userNotification = createUserNotification();

        userNotification.setNotificationCampaignsCreateDate(Arrays.asList(23456L, 76767L));

        when(userNotificationRepository.findBySsn(SSN)).thenReturn(Mono.just(userNotification));

        // when
        NoticeService noticeService = new NoticeServiceImpl(notificationRepository, userNotificationRepository, reactiveMongoTemplate);
        Mono<UserNoticeListResDto> userNoticeListResDtoMono = noticeService.userNoticeList(SSN, TYPE, PAGE, null);

        // then
        StepVerifier.create(userNoticeListResDtoMono)
                .expectNextMatches(res -> {
                    return  res.getCurrentPage() == PAGE
                            && res.getTotalPages() == 1
                            && res.getNotifications().size() == 2
                            && res.getLastSeenCampaign() == 12345L
                            && res.getLastSeenTransaction() == 12346L;
                })
                .verifyComplete();
    }

    @Test
    void shouldSetAndReturnUserLastSeenIdForGivenSSNAndLastSeenCampAndTransaction() {
        Long lastSeenCamp = 12345L;
        Long lastSeenTra = 12346L;

        UserNotification userNotification = createUserNotification();
        userNotification.setNotificationCampaignsCreateDate(Arrays.asList(12345L));

        when(userNotificationRepository.findBySsn(SSN)).thenReturn(Mono.just(userNotification));

        // when
        NoticeService noticeService = new NoticeServiceImpl(notificationRepository, userNotificationRepository, reactiveMongoTemplate);
        Mono<LastSeenResDto> userNoticeListResDtoMono = noticeService.userLastSeenId(SSN, lastSeenCamp, lastSeenTra);

        when(userNotificationRepository.save(userNotification)).thenReturn(Mono.just(userNotification));

        // then
        StepVerifier.create(userNoticeListResDtoMono)
                .expectNextMatches(res -> {
                    return  res.getLastSeenCampaign().equals(lastSeenCamp)
                            && res.getLastSeenTransaction().equals(lastSeenTra);
                })
                .verifyComplete();
    }

    @Test
    void shouldReturnUnreadNoticeCountForGivenSSN() {

        UserNotification userNotification = createUserNotification();
        userNotification.setNotificationCampaignsCreateDate(List.of(12345L));

        when(userNotificationRepository.findBySsn(SSN)).thenReturn(Mono.just(userNotification));

        // when
        NoticeService noticeService = new NoticeServiceImpl(notificationRepository, userNotificationRepository, reactiveMongoTemplate);
        Mono<UnreadNoticeCountResDto> unreadNoticeCountResDtoMono = noticeService.unreadNoticeCount(SSN, "android");

        // then
        StepVerifier.create(unreadNoticeCountResDtoMono)
                .expectNextMatches(res -> {
                    return  res.getRemainNotificationCount() == 0
                            && res.getLastSeenCampaign().equals(userNotification.getLastSeenCampaign())
                            && res.getLastSeenTransaction().equals(userNotification.getLastSeenTransaction());
                })
                .verifyComplete();
    }

    // not compeleted
    @Test
    void delete() {
        UserNotification userNotification = createUserNotification();
        userNotification.setNotificationCampaignsCreateDate(Arrays.asList(12345L));

        when(userNotificationRepository.findBySsn(SSN)).thenReturn(Mono.just(userNotification));

        DeleteNoticeReqDto dto = new DeleteNoticeReqDto();
        dto.setNotificationsByCreationDate(Arrays.asList(12305L));
        dto.setNotificationType("campaign");

        // when
        NoticeService noticeService = new NoticeServiceImpl(notificationRepository, userNotificationRepository, reactiveMongoTemplate);
        Mono<UserNotification> userNotificationDtoMono = noticeService.deleteMultiNotice(SSN, dto);

        when(userNotificationRepository.save(userNotification)).thenReturn(userNotificationDtoMono);

        StepVerifier.create(userNotificationDtoMono)
                .expectNextMatches(res ->{
                    return res.getNotificationCampaignsCreateDate().size() == 1;
                })
                .verifyComplete();
    }

    @Test
    void resetNoticeCountJob() {
    }
}