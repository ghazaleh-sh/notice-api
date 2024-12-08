package ir.co.sadad.noticeapi.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(title = "آبجکت پاسخ اعلان سرویس بیرونی")
@Data
public class sendThirdPartyResDto {

    @Schema(title = "تاریخ ایجاد اعلان- creationDate")
    private Long notificationId;
}
