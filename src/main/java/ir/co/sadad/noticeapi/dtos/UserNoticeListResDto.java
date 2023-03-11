package ir.co.sadad.noticeapi.dtos;


import io.swagger.v3.oas.annotations.media.Schema;
import ir.co.sadad.noticeapi.models.Notification;
import lombok.Data;

import java.util.List;

@Schema(title = "آبجکت لیست پیام های کاربر")
@Data
public class UserNoticeListResDto {

    @Schema(title = "کد ملی")
    private String ssn;

    @Schema(title = "شناسه آخرین پیام دیده شده توسط کاربر")
    private String lastSeenId;

    @Schema(title = "لیست پیام ها")
    private List<Notification> notifications;
}
