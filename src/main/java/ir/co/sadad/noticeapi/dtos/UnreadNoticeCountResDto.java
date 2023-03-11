package ir.co.sadad.noticeapi.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(title = "آبجکت پاسخ تعداد پیامهای خوانده نشده")
@Data
public class UnreadNoticeCountResDto {

    @Schema(title = "تعداد پیام های خوانده نشده")
    private Long notificationCount;

    @Schema(title = "کدملی کاربر")
    private String ssn;
}
