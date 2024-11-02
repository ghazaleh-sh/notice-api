package ir.co.sadad.noticeapi.repositories;

import ir.co.sadad.noticeapi.enums.NotificationStatus;
import ir.co.sadad.noticeapi.enums.Platform;
import ir.co.sadad.noticeapi.models.Notification;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface NotificationRepository extends ReactiveMongoRepository<Notification, String> {

    Mono<Notification> findByCreationDate(Long creationDate);

    Flux<Notification> findByType(String type);

    default Query findByCreationDateAndPlatformAndStatusIsAndActivationDate(Long creationDate, Platform platform, String currentDate) {
        Criteria criteria = new Criteria().andOperator(
                Criteria.where("creationDate").is(creationDate),
                Criteria.where("status").is(NotificationStatus.ACTIVE),
                new Criteria().orOperator(
                        Criteria.where("activationDate").is(null),
                        Criteria.where("activationDate").lte(currentDate)
                )
        );
        if (platform != null)
            criteria = new Criteria().andOperator(criteria, Criteria.where("platform").in(platform, Platform.ALL));
        else
            criteria = new Criteria().andOperator(criteria, Criteria.where("platform").is(Platform.ALL));

        return new Query(criteria);
    }

}
