package ir.co.sadad.noticeapi.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import java.io.Serializable;

@Schema(title = "آبجکت درخواست اعلان کمپین")
@Data
public class SendCampaignNoticeReqDto implements Serializable {
    private static final long serialVersionUID = 1678659015284581484L;

    @Schema(title = "عنوان اعلان")
    @NotBlank(message = "title.is.required")
    private String title;

    @Schema(title = "توضیحات اعلان")
    @NotBlank(message = "description.is.required")
    private String description;

//    @Schema(title = "تاریخ اعلان-epoch")
//    @NotNull(message = "date.is.required")
//    private Long date;

    @Schema(title = "کانال اعلان")
    @NotBlank(message = "platform.is.required")
    @Pattern(regexp = "^(ANDROID|IOS|PWA)$", message = "platform.is.not.valid")
    private String platform;

    private String ssn;
}
