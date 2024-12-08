package ir.co.sadad.noticeapi.dtos;

import lombok.Data;

@Data
public class LastSeenReqDto {

    private Long lastSeenCampaign;
    private Long lastSeenTransaction;
}
