package ir.co.sadad.noticeapi.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(title = "آبجکت آخرین پیام های دیده شده")
@Data
public class LastSeenResDto {

    @Schema(title = "تاریخ ایجاد مربوط به آخرین پیام دیده شده کمپین توسط کاربر")
    private Long lastSeenCampaign;

    @Schema(title = "تاریخ ایجاد مربوط به آخرین پیام دیده شده تراکنش ها توسط کاربر")
    private Long lastSeenTransaction;
}
