package ir.co.sadad.noticeapi.dtos;

import lombok.Data;

import java.util.List;


@Data
public class SendCampaignNoticeResDto {

//    private String multicastId;
    private String success;
    private String failure;
    private List<String> failureResults;

}
