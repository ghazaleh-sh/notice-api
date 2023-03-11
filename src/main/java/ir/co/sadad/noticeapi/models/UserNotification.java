package ir.co.sadad.noticeapi.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

@Document
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserNotification implements Serializable {

    private static final long serialVersionUID = 5955078288357146342L;
    @Id
    private String id;

    /**
     * user national code
     */
    @NotNull(message = "ssn.must.not.be.null")
    private String ssn;

    /**
     * notifications list of user
     */
    private List<Notification> notifications;

    /**
     * id of last seen notice
     */
    private String lastSeenNotificationId;

    /**
     *id of previous notice which seen by user
     */
    private String previousNotificationId;

    /**
     * unread notices count
     */
    private Long remainNotificationCount;
}
