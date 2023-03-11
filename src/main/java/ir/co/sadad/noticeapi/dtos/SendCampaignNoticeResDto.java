package ir.co.sadad.noticeapi.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(title = "آبجکت پاسخ اعلانات کمپین")
@Data
public class SendCampaignNoticeResDto {

    @Schema(title = "شناسه پیام ارسالی")
    private String notificationId;

    @Schema(title = "تعداد پیام های موفق ارسالی")
    private String success;

    @Schema(title = "تعداد پیام های ناموفق ارسالی")
    private String failure;

    @Schema(title = "لیست شناسه پیام های ناموفق")
    private List<String> failureResults;

}
