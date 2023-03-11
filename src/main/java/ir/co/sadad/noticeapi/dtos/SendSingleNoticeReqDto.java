package ir.co.sadad.noticeapi.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Schema(title = "آبجکت تک اعلان")
@Data
public class SendSingleNoticeReqDto implements Serializable {

    private static final long serialVersionUID = 7044673541516162886L;

    @Schema(title = "عنوان اعلان")
    @NotNull(message = "title.is.required")
    private String title;

    @Schema(title = "توضیحات اعلان")
    @NotNull(message = "description.is.required")
    private String description;

    @Schema(title = "تاریخ اعلان-epoch")
    @NotNull(message = "date.is.required")
    private Long date;
}
