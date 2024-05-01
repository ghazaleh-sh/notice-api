package ir.co.sadad.noticeapi.dtos;


import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PanelNoticeListReqDto {
    private int pageNumber;
    private int pageSize;
    private String platform;
    private String title;
    private String type;
    private String dateFrom;
    private String dateTo;
    private String sortBy;
    private String sort;
}
