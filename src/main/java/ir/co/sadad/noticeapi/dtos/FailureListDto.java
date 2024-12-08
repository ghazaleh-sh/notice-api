package ir.co.sadad.noticeapi.dtos;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class FailureListDto {

    List<String> failure;
}
