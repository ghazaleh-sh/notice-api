package ir.co.sadad.noticeapi.dtos;


import ir.co.sadad.noticeapi.models.Notification;
import lombok.Data;

import java.util.List;

@Data
public class UserNoticeListResDto {

    private String ssn;
    private List<Notification> notifications;
    private String lastSeenId;
}
