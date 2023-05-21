package ir.co.sadad.noticeapi.dtos;


import lombok.Data;

import java.util.List;

@Data
public class DeleteNoticeReqDto {

    private List<Long> notificationsByCreationDate;
    private String notificationType;
}
