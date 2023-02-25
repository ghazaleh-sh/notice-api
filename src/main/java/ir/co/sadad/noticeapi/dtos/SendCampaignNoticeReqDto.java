package ir.co.sadad.noticeapi.dtos;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class SendCampaignNoticeReqDto extends SendSingleNoticeReqDto{
    private static final long serialVersionUID = 1678659015284581484L;

    /*
    if ssn is null, send for all
     */
    private List<String> ssn;
}
