package ir.co.sadad.noticeapi.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import ir.co.sadad.noticeapi.enums.NotificationStatus;
import ir.co.sadad.noticeapi.enums.Platform;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Document
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Notification implements Serializable {

    @Serial
    private static final long serialVersionUID = -1410604679815168078L;
    @Id
    private String id;

    /**
     * date of notice release - used for transactions
     */
//    @NotNull(message = "date.must.not.be.null")
    private String date;

    /**
     * date of notice created into userNotification collection - equivalent to System.currentTimeMillis()
     */
    private Long creationDate;

    /**
     * title of notice - for campaign notices
     */
    private String title;

    /**
     * body of the notice - for campaign notices
     */
    private String description;

    /**
     * type = 1 means transactions less than 30k
     * type = 2 means Campaign notices
     */
    @NotNull(message = "type.must.not.be.null")
    private String type;

    /**
     * balance of notice-for under 30 T transactions
     */
    private String balance;

    /**
     * account of notice-for under 30 T transactions
     */
    private String account;

    /**
     * withdraw of notice-for under 30 T transactions
     */
    private String withdraw;

    /**
     * bankName of notice-for under 30 T transactions
     */
    private String bankName;

    /**
     * transaction type-for under 30 T transactions
     */
    private String transactionType;

    /**
     * channels for separating messages
     */
    private Platform platform;

    private String creationDateUTC;

    /**
     * message creator - ssn
     */
    @CreatedBy
    private String createdBy;

    /**
     * message modifier through update panel service - ssn
     */
    @LastModifiedBy
    private String modifiedBy;

    private NotificationStatus status;

    private String successNumber;

    private String failureNumber;

    @JsonIgnore
    private List<String> failureList;

    private String activationDate;

    private String hyperlink;

    private Boolean pushNotification;

    private List<String> succeededListForFuturePush;

}
