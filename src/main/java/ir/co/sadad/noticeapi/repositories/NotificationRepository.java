package ir.co.sadad.noticeapi.repositories;

import ir.co.sadad.noticeapi.models.Notification;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends ReactiveMongoRepository<Notification, String> {
}
