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
     * notifications list of user for single notices
     */
    private List<Notification> notificationTransactions;

    /**
     * notifications list of user for campaign notices - just keep creationDate of each notification record
     */
    private List<Long> notificationCampaignsCreateDate;

    /**
     * creation date of last seen campaign notice
     */
    private Long lastSeenCampaign;

    /**
     * creation date of last seen transaction notice
     */
    private Long lastSeenTransaction;

    /**
     * unread notices count
     */
    private Integer remainNotificationCount;

    /**
     * all user's notifications count
     */
    private Integer notificationCount;
}
