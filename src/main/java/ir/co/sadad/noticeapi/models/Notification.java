package ir.co.sadad.noticeapi.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Document
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Notification implements Serializable {

    private static final long serialVersionUID = -1410604679815168078L;
    @Id
    private String id;

    /**
     * date of notice release- base on epoch time (milliseconds)
     */
    @NotNull(message = "date.must.not.be.null")
    private Long date;

    /**
     * date of notice created into userNotification collection - equivalent to System.currentTimeMillis()
     */
    private Long creationDate;

    /**
     * title of notice - for campaign notices
     */
//    @NotNull(message = "title.must.not.be.null")
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
     * balance of notice- for under 30 T transactions
     */
    private String balance;

    /**
     * account of notice- for under 30 T transactions
     */
    private String account;

    /**
     * withdraw of notice- for under 30 T transactions
     */
    private String withdraw;

    /**
     * bankName of notice- for under 30 T transactions
     */
    private String bankName;
}
