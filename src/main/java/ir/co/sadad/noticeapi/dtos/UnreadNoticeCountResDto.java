package ir.co.sadad.noticeapi.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(title = "آبجکت پاسخ تعداد پیامهای خوانده نشده")
@Data
public class UnreadNoticeCountResDto {

    @Schema(title = "تعداد پیام های خوانده نشده")
    private Integer remainNotificationCount;

    @Schema(title = "تاریخ ایجاد مربوط به آخرین پیام دیده شده کمپین توسط کاربر")
    private Long lastSeenCampaign;

    @Schema(title = "تاریخ ایجاد مربوط به آخرین پیام دیده شده تراکنش ها توسط کاربر")
    private Long lastSeenTransaction;
}
