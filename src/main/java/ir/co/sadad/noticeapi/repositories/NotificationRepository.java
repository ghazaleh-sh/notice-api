package ir.co.sadad.noticeapi.repositories;

import ir.co.sadad.noticeapi.models.Notification;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface NotificationRepository extends ReactiveMongoRepository<Notification, String> {

    Mono<Notification> findByDateAndType(Long date, String type);
}
