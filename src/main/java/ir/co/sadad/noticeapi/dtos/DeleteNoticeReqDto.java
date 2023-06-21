package ir.co.sadad.noticeapi.dtos;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class DeleteNoticeReqDto {

    private List<Long> notificationsByCreationDate;

    @Schema(title = "نوع اعلان")
    @NotNull(message = "type.is.required")
    private String notificationType;
}
