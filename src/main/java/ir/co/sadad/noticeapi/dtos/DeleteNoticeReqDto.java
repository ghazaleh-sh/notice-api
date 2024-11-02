package ir.co.sadad.noticeapi.dtos;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.util.List;

@Data
public class DeleteNoticeReqDto {

    private List<Long> notificationsByCreationDate;

    @Schema(title = "نوع اعلان")
    @NotBlank(message = "type.is.required")
    private String notificationType;
}
