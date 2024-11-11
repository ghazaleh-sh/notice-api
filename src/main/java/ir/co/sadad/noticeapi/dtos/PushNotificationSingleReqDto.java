package ir.co.sadad.noticeapi.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class PushNotificationSingleReqDto {

    private String ssn;

    @NotBlank(message = "title.is.required")
    private String title;

    @NotBlank(message = "description.is.required")
    private String description;

    @Schema(title = "کانال اعلان")
    private String platform;

    @Schema(title = "لینک مربوط به اعلان در صورت وجود")
    private String hyperlink;

    private String noticeType;
}
