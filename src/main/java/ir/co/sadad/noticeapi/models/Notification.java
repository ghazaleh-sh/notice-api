package ir.co.sadad.noticeapi.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;

@Document
@Data
@AllArgsConstructor
@Builder
public class Notification implements Serializable {

    private static final long serialVersionUID = -1410604679815168078L;
    @Id
    private String id;

    /**
     * title of notice
     */
    private String title;

    /**
     * body of the notice
     */
    private String description;

    /**
     * date of notice release- base on epoch time (milliseconds )
     */
    private Long date;

    /**
     * type = 1 means transactions less than 30k
     * type = 2 means Campaign notices
     */
    private String type;
}
