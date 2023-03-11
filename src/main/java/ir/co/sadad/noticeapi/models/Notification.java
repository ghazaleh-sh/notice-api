package ir.co.sadad.noticeapi.models;

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
public class Notification implements Serializable {

    private static final long serialVersionUID = -1410604679815168078L;
    @Id
    private String id;

    /**
     * title of notice
     */
    @NotNull(message = "title.must.not.be.null")
    private String title;

    /**
     * body of the notice
     */
    private String description;

    /**
     * date of notice release- base on epoch time (milliseconds )
     */
    @NotNull(message = "date.must.not.be.null")
    private Long date;

    /**
     * type = 1 means transactions less than 30k
     * type = 2 means Campaign notices
     */
    @NotNull(message = "type.must.not.be.null")
    private String type;
}
