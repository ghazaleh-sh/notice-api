package ir.co.sadad.noticeapi.services;

import ir.co.sadad.noticeapi.dtos.*;
import ir.co.sadad.noticeapi.enums.NoticeType;
import ir.co.sadad.noticeapi.enums.NotificationStatus;
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
import org.modelmapper.ModelMapper;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.data.mongodb.core.ReactiveMongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;


@Slf4j
@RequiredArgsConstructor
@Service
public class PanelNoticeServiceImpl implements PanelNoticeService {

    private final NotificationRepository notificationRepository;

    private final UserNotificationRepository userNotificationRepository;

    private final ReactiveMongoOperations reactiveMongoOperations;

    private final ModelMapper modelMapper;

    private List<String> ssnList = new ArrayList<>();

    private final AtomicInteger success = new AtomicInteger();
    private final AtomicInteger failure = new AtomicInteger();

    private final PushNotificationProviderService pushNotificationService;

    @Override
    @SneakyThrows
    public Mono<SendCampaignNoticeResDto> sendCampaignNotice(SendCampaignNoticeReqDto campaignNoticeReqDto, FilePart file) {
        SendCampaignNoticeResDto res = new SendCampaignNoticeResDto();

        List<String> failRes = new ArrayList<>();
        success.set(0);
        failure.set(0);

        if (file == null)
            return saveNotification(campaignNoticeReqDto, null, 0, Collections.emptyList(), NoticeType.GENERAL)
                    .flatMap(savedNotification -> {
                        res.setNotificationId(savedNotification.getId());
                        res.setFailure(savedNotification.getFailureNumber());
                        res.setSuccess(savedNotification.getSuccessNumber());
                        res.setFailureResults(failRes);
                        return Mono.just(res);
                    });

        else
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
                                    return Mono.error(new GeneralException("ssn.file.invalid", HttpStatus.BAD_REQUEST));
                                }
                            }))
//                            .switchIfEmpty(Mono.defer(() -> saveNotification(campaignNoticeReqDto, null, 0, NoticeType.GENERAL))))
                    .flatMap(o -> saveNotification(campaignNoticeReqDto, failRes, ssnList.size(), ssnList, NoticeType.CAMPAIGN))
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


    private Mono<Notification> saveNotification(SendCampaignNoticeReqDto campaignNoticeReqDto, List<String> failRes,
                                                int successNumber, List<String> successSsn, NoticeType noticeType) {

        if (campaignNoticeReqDto.getPushNotification().compareTo(true) == 0) {
            if (campaignNoticeReqDto.getActivationDate() == null ||
                    campaignNoticeReqDto.getActivationDate().equals("cuu")) {
                PushNotificationReqDto pushReqDto = new PushNotificationReqDto();
                modelMapper.map(campaignNoticeReqDto, pushReqDto);
                pushReqDto.setSuccessSsn(successSsn);

                pushNotificationService.multiCastPushNotification(pushReqDto)
                        .subscribe(); // Fire-and-forget: This triggers the execution but immediately "forgets", triggers the HTTP request without waiting for its result, continuing the flow immediately.
                //No execution happens until something subscribes to the reactive source (like a Mono or Flux)
            }
        }


        return Mono //where you return a Mono or Flux, Spring WebFlux or other reactive frameworks will handle subscription for you
                .just(campaignNoticeReqDto)
                .flatMap(camp -> notificationRepository.insert(Notification
                        .builder()
                        .creationDate(System.currentTimeMillis())
                        .description(camp.getDescription())
                        .title(camp.getTitle())
                        .type(noticeType.getValue())
                        .platform(camp.getPlatform() != null ? Platform.valueOf(camp.getPlatform())
                                : Platform.ALL)
                        .createdBy(camp.getSsn())
                        .creationDateUTC(Utilities.getCurrentUTC())
                        .status(NotificationStatus.ACTIVE)
                        .successNumber(String.valueOf(successNumber))
                        .failureNumber(String.valueOf(failure))
                        .failureList(failRes)
                        .activationDate(camp.getActivationDate())
                        .hyperlink(camp.getHyperlink())
                        .pushNotification(camp.getPushNotification())
                        .build()))
                .onErrorMap(throwable -> new ValidationException(throwable.getMessage(), "error.on.save.notification"));
    }

    public Mono<UserNotification> saveUser(String ssn, Long savedNotificationId) {
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
    public Mono<Notification> updateCampaignMessage(UpdateCampaignNoticeDto updateCampaignDto, String ssn) {
        return notificationRepository.findByCreationDate(updateCampaignDto.getCreationDate())
                .switchIfEmpty(Mono.error(new GeneralException("notification.not.find", HttpStatus.NOT_FOUND)))
                .flatMap(notification -> {
                    modelMapper.map(updateCampaignDto, notification);
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

    @Override
    public Mono<FailureListDto> failureNotifications(Long creationDate) {
        return notificationRepository.findByCreationDate(creationDate)
                .switchIfEmpty(Mono.error(new GeneralException("notification.not.find", HttpStatus.NOT_FOUND)))
                .flatMap(notification -> {
                    List<String> failureList = notification.getFailureList();
                    if (failureList == null)
                        return Mono.empty();

                    return Mono.just(FailureListDto.builder().failure(failureList).build());
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
