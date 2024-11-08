package ir.co.sadad.noticeapi.dtos;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;


@Data
public class UpdateCampaignNoticeDto implements Serializable {

    @Serial
    private static final long serialVersionUID = -5038244121738497539L;

    private Long creationDate;
    private String title;
    private String description;
    private String status;
    private String activationDate;
    private String hyperlink;
}
