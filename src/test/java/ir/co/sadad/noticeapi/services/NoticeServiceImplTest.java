package ir.co.sadad.noticeapi.services;

import ir.co.sadad.noticeapi.dtos.LastSeenResDto;
import ir.co.sadad.noticeapi.dtos.UserNoticeListResDto;
import ir.co.sadad.noticeapi.models.Notification;
import ir.co.sadad.noticeapi.models.UserNotification;
import ir.co.sadad.noticeapi.repositories.NotificationRepository;
import ir.co.sadad.noticeapi.repositories.UserNotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Arrays;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NoticeServiceImplTest {

    @Mock
    private UserNotificationRepository userNotificationRepository;

    @Mock
    private NotificationRepository notificationRepository;


    @Test
    void sendCampaignNotice() {
    }

    @Test
    void userNoticeList() {
        // given
        String ssn = "123456789";
        String type = "2";
        int page = 1;
        UserNotification userNotification = new UserNotification("1",ssn,null,null,-1L,-1L,2,2);
        userNotification.setNotificationTransactions(Arrays.asList(
                new Notification("1", 213L,4444L,null,null,"1","5000","34567677","1000","lgd"),
                new Notification("2", 224L,4445L,"reminder","descriptionnnn","2",null,null,null,null)
        ));

        when(userNotificationRepository.findBySsn(ssn)).thenReturn(Mono.just(userNotification));

        // when
        NoticeService noticeService = new NoticeServiceImpl(notificationRepository, userNotificationRepository);
        Mono<UserNoticeListResDto> userNoticeListResDtoMono = noticeService.userNoticeList(ssn, type, page);

        // then
        StepVerifier.create(userNoticeListResDtoMono)
                .expectNextMatches(res -> {
                    return res.getSsn().equals(ssn)
                            && res.getCurrentPage() == page
                            && res.getTotalPages() == 1
                            && res.getNotifications().size() == 1
                            && res.getLastSeenCampaign() == -1L;
                })
                .verifyComplete();
    }

    @Test
    void userLastSeenId() {
        String ssn = "123456789";
        Long lastSeenCamp = 12345L;
        Long lastSeenTra = 12345L;
        UserNotification userNotification = new UserNotification("1",ssn,null,null,-1L,-1L,2,2);
        userNotification.setNotificationTransactions(Arrays.asList(
                new Notification("1", 213L,4444L,null,null,"1","5000","34567677","1000","lgd"),
                new Notification("2", 224L,4445L,"reminder","descriptionnnn","2",null,null,null,null)
        ));

        when(userNotificationRepository.findBySsn(ssn)).thenReturn(Mono.just(userNotification));

        // when
        NoticeService noticeService = new NoticeServiceImpl(notificationRepository, userNotificationRepository);
        Mono<LastSeenResDto> userNoticeListResDtoMono = noticeService.UserLastSeenId(ssn, lastSeenCamp, lastSeenTra);

        // then
        StepVerifier.create(userNoticeListResDtoMono)
                .expectNextMatches(res -> {
                    return res.getSsn().equals(ssn)
                            && res.getLastSeenCampaign().equals(lastSeenCamp)
                            && res.getLastSeenTransaction().equals(lastSeenTra);
                })
                .verifyComplete();
    }

    @Test
    void unreadNoticeCount() {
    }

    @Test
    void clearUnreadCount() {
    }

    @Test
    void resetNoticeCountJob() {
    }
}