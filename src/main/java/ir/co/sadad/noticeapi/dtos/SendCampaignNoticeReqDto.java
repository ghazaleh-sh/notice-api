package ir.co.sadad.noticeapi.dtos;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class SendCampaignNoticeReqDto {

    @NotNull(message = "title.is.required")
    private String title;

    @NotNull(message = "description.is.required")
    private String description;

    private List<String> ssn;

    @NotNull(message = "date.is.required")
    private Integer date;
}
