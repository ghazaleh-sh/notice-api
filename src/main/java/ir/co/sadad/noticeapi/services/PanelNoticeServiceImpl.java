package ir.co.sadad.noticeapi.services;

import ir.co.sadad.noticeapi.dtos.UpdateCampaignNoticeDto;
import ir.co.sadad.noticeapi.exceptions.GeneralException;
import ir.co.sadad.noticeapi.models.Notification;
import ir.co.sadad.noticeapi.repositories.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@RequiredArgsConstructor
@Service
public class PanelNoticeServiceImpl implements PanelNoticeService {

    private final NotificationRepository notificationRepository;

    @Override
    public Mono<Notification> updateCampaignMessage(UpdateCampaignNoticeDto updateCampaignDto) {
        return notificationRepository.findByCreationDate(updateCampaignDto.getCreationDate())
                .switchIfEmpty(Mono.error(new GeneralException("notification.not.find", HttpStatus.NOT_FOUND)))
                .flatMap(notification -> {
                    notification.setTitle(updateCampaignDto.getTitle());
                    notification.setDescription(updateCampaignDto.getDescription());
                    return Mono.just(notification);
                })
                .flatMap(notificationRepository::save);

    }

    @Override
    public Mono<Void> deleteCampaignMessage(Long notificationId) {
        return notificationRepository.findByCreationDate(notificationId)
                .switchIfEmpty(Mono.error(new GeneralException("notification.not.find", HttpStatus.NOT_FOUND)))
                .flatMap(notification -> notificationRepository.deleteById(notification.getId()))
                .onErrorResume(e -> {
                    log.error("Error occurred during record deletion: " + e.getMessage());
                    e.printStackTrace();
                    return Mono.error(e);
                })
                .doOnSuccess(v -> {
                    log.info("Record deleted successfully");
                })
                .then();

    }
}
