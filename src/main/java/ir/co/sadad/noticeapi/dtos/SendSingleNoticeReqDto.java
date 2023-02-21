package ir.co.sadad.noticeapi.dtos;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Data
public class SendSingleNoticeReqDto implements Serializable {

    private static final long serialVersionUID = 7044673541516162886L;
    @NotNull(message = "title.is.required")
    private String title;

    @NotNull(message = "description.is.required")
    private String description;

    @NotNull(message = "date.is.required")
    private Long date;
}
