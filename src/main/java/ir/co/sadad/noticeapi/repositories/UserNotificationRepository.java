package ir.co.sadad.noticeapi.repositories;

import ir.co.sadad.noticeapi.models.UserNotification;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

public interface UserNotificationRepository extends ReactiveMongoRepository<UserNotification, String> {
}
