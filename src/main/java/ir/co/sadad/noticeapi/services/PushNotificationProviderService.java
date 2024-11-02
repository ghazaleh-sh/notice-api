package ir.co.sadad.noticeapi.services;

import ir.co.sadad.noticeapi.dtos.PushNotificationReqDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@RequiredArgsConstructor
@Service
public class PushNotificationProviderService {

    private final WebClient webClient;

    @Value(value = "${push-notification.multicast-path}")
    private String multicastPushPath;

    @Value(value = "${push-notification.single-path}")
    private String singlePushPath;


    public Mono<Void> multiCastPushNotification(PushNotificationReqDto notificationReqDto) {
        return webClient
                .post()
                .uri(multicastPushPath)
                .body(Mono.just(notificationReqDto), PushNotificationReqDto.class)
                .retrieve()
                .bodyToMono(Void.class)
                .doOnError(e -> log.error("Failed to send campaign push notification", e));
    }

    public Mono<Void> singlePushNotification(PushNotificationReqDto notificationReqDto) {
        return webClient
                .post()
                .uri(singlePushPath)
                .body(Mono.just(notificationReqDto), PushNotificationReqDto.class)
                .retrieve()
                .bodyToMono(Void.class)
                .doOnError(e -> log.error("Failed to send third-party single push notification", e));
    }
}
