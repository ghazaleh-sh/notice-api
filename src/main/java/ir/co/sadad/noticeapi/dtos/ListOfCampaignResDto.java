package ir.co.sadad.noticeapi.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import ir.co.sadad.noticeapi.models.Notification;
import lombok.Data;

import java.util.List;

@Data
public class ListOfCampaignResDto {

    @Schema(title = "لیست پیام ها")
    private List<Notification> notifications;
}
