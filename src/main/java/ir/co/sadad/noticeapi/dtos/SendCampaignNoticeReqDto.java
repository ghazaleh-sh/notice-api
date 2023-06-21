package ir.co.sadad.noticeapi.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

@Schema(title = "آبجکت درخواست اعلان کمپین")
@Data
public class SendCampaignNoticeReqDto implements Serializable {
    private static final long serialVersionUID = 1678659015284581484L;

    @Schema(title = "عنوان اعلان")
    @NotNull(message = "title.is.required")
    private String title;

    @Schema(title = "توضیحات اعلان")
    @NotNull(message = "description.is.required")
    private String description;

//    @Schema(title = "تاریخ اعلان-epoch")
//    @NotNull(message = "date.is.required")
//    private Long date;
}
