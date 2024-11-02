package ir.co.sadad.noticeapi.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import java.io.Serial;
import java.io.Serializable;

@Schema(title = "آبجکت درخواست اعلان کمپین")
@Data
public class SendCampaignNoticeReqDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 1678659015284581484L;

    @Schema(title = "عنوان اعلان")
    @NotBlank(message = "title.is.required")
    private String title;

    @Schema(title = "توضیحات اعلان")
    @NotBlank(message = "description.is.required")
    private String description;

    @Schema(title = "کانال اعلان")
//    @NotBlank(message = "platform.is.required")
//    @Pattern(regexp = "^(ANDROID|IOS|PWA|ALL)$", message = "platform.is.not.valid")
    private String platform;

    @Schema(title = "در صورت ارسال اعلان تکی: کدملی کاربر، در صورت اعلان کمپین: کدملی ایجاد کننده اعلان")
    private String ssn;

    @Schema(title = "تاریخ فعالسازی اعلان")
    private String activationDate;

    @Schema(title = "لینک مربوط به اعلان در صورت وجود")
    private String hyperlink;

    @Schema(title = "آیا همراه این اعلان پوش نوتیفیکشن هم برود؟")
    private Boolean pushNotification;
}
