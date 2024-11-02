package ir.co.sadad.noticeapi.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.util.List;

@Schema(title = "آبجکت درخواست ارسال پوش نوتیفیکیشن اعلان کمپین")
@Data
public class PushNotificationReqDto {

    private List<String> successSsn;

    @NotBlank(message = "title.is.required")
    private String title;

    @NotBlank(message = "description.is.required")
    private String description;

    @Schema(title = "کانال اعلان")
    private String platform;

    @Schema(title = "تاریخ فعالسازی اعلان")
    private String activationDate;

    @Schema(title = "لینک مربوط به اعلان در صورت وجود")
    private String hyperlink;
}
