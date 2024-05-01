package ir.co.sadad.noticeapi.services;

import ir.co.sadad.noticeapi.dtos.ListOfCampaignResDto;
import ir.co.sadad.noticeapi.dtos.PanelNoticeListReqDto;
import ir.co.sadad.noticeapi.dtos.UpdateCampaignNoticeDto;
import ir.co.sadad.noticeapi.exceptions.GeneralException;
import ir.co.sadad.noticeapi.models.Notification;
import ir.co.sadad.noticeapi.repositories.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.ReactiveMongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Comparator;
import java.util.List;


@Slf4j
@RequiredArgsConstructor
@Service
public class PanelNoticeServiceImpl implements PanelNoticeService {

    private final NotificationRepository notificationRepository;

    private final ReactiveMongoOperations reactiveMongoOperations;

    @Override
    public Mono<Notification> updateCampaignMessage(UpdateCampaignNoticeDto updateCampaignDto, String ssn) {
        return notificationRepository.findByCreationDate(updateCampaignDto.getCreationDate())
                .switchIfEmpty(Mono.error(new GeneralException("notification.not.find", HttpStatus.NOT_FOUND)))
                .flatMap(notification -> {
                    if (updateCampaignDto.getTitle() != null) notification.setTitle(updateCampaignDto.getTitle());
                    if (updateCampaignDto.getDescription() != null) notification.setDescription(updateCampaignDto.getDescription());
                    notification.setModifiedBy(ssn);
                    return Mono.just(notification);
                })
                .flatMap(notificationRepository::save);

    }

    @Override
    public Mono<Void> deleteCampaignMessage(Long creationDate) {
        return notificationRepository.findByCreationDate(creationDate)
                .switchIfEmpty(Mono.error(new GeneralException("notification.not.find", HttpStatus.NOT_FOUND)))
                .flatMap(notification -> notificationRepository.deleteById(notification.getId()))
                .onErrorResume(e -> {
                    log.error("Error occurred during record deletion: " + e.getMessage());
                    e.printStackTrace();
                    return Mono.error(e);
                })
                .doOnSuccess(v -> log.info("Record deleted successfully"))
                .then();

    }

    @Override
    public Mono<ListOfCampaignResDto> campaignList(PanelNoticeListReqDto req) {
        return setAdvancedSearchParams(req)
                .collectList()
                .flatMap(noticeFilteredList -> {
                    int totalItems = noticeFilteredList.size();
                    int startIndex = (req.getPageNumber() - 1) * req.getPageSize();
                    int endIndex = Math.min(startIndex + req.getPageSize(), totalItems);

                    startIndex = Math.max(startIndex, 0);  // ensure startIndex is not negative
                    endIndex = Math.min(endIndex, totalItems);

                    List<Notification> sublist;
                    try {
                        sublist = noticeFilteredList.subList(startIndex, endIndex);
                    } catch (Exception e) {
                        return Mono.error(new GeneralException("INVALID.INDEX.RANGE", HttpStatus.BAD_REQUEST));
                    }

                    ListOfCampaignResDto res = new ListOfCampaignResDto();
                    if (!sublist.isEmpty()) {
                        res.setNotifications(sublist);
                    }
                    return Mono.just(res);
                });
    }

    private Flux<Notification> setAdvancedSearchParams(PanelNoticeListReqDto req) {
        Criteria criteria = new Criteria();
        if (req.getPlatform() != null && !req.getPlatform().isEmpty()) {
            criteria.and("platform").in(req.getPlatform());
        }
        if (req.getTitle() != null && !req.getTitle().isEmpty()) {
            criteria.and("title").in(req.getTitle());
        }
        if (req.getType() != null && !req.getType().isEmpty()) {
            criteria.and("type").in(req.getType());
        }
        if (req.getDateFrom() != null && !req.getDateFrom().isEmpty() && (req.getDateTo() == null || req.getDateTo().isEmpty())) {
            criteria.and("creationDateUTC").gte(req.getDateFrom());
        }
        if (req.getDateTo() != null && !req.getDateTo().isEmpty() && (req.getDateFrom() == null || req.getDateFrom().isEmpty())) {
            criteria.and("creationDateUTC").lte(req.getDateTo());
        }
        if (req.getDateFrom() != null && !req.getDateFrom().isEmpty() && req.getDateTo() != null && !req.getDateTo().isEmpty()) {
            criteria.and("creationDateUTC").gte(req.getDateFrom()).lte(req.getDateTo());
        }

        Comparator<Notification> comparator;
        if (req.getSortBy() != null && !req.getSortBy().isEmpty()) {
            comparator = switch (req.getSortBy()) {
                case "platform" -> Comparator.comparing(Notification::getPlatform).reversed();
                case "title" -> Comparator.comparing(Notification::getTitle).reversed();
                case "type" -> Comparator.comparing(Notification::getType).reversed();
                case "creationDate" -> Comparator.comparingLong(Notification::getCreationDate).reversed();
                default -> throw new GeneralException("Invalid.sortBy", HttpStatus.BAD_REQUEST);
            };

        } else comparator = Comparator.comparing(Notification::getCreationDateUTC); //نزولی دیفالت

        if (req.getSort() != null && req.getSort().equals("asc"))
            comparator = comparator.reversed();

        Query query = Query.query(criteria);
        return reactiveMongoOperations.find(query, Notification.class)
                .sort(comparator);
    }

}
