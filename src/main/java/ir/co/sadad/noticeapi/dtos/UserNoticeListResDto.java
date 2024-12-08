package ir.co.sadad.noticeapi.dtos;


import io.swagger.v3.oas.annotations.media.Schema;
import ir.co.sadad.noticeapi.models.Notification;
import lombok.Data;

import java.util.List;

@Schema(title = "آبجکت لیست پیام های کاربر")
@Data
public class UserNoticeListResDto {

    @Schema(title = "تاریخ ایجاد مربوط به آخرین پیام دیده شده کمپین توسط کاربر")
    private Long lastSeenCampaign;

    @Schema(title = "تاریخ ایجاد مربوط به آخرین پیام دیده شده تراکنش ها توسط کاربر")
    private Long lastSeenTransaction;

    @Schema(title = "لیست پیام ها")
    private List<Notification> notifications;

    @Schema(title = "تعداد کل صفحات")
    private Integer totalPages;

    @Schema(title = "شماره صفحه جاری")
    private Integer currentPage;
}
