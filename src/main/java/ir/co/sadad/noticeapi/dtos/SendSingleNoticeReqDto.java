package ir.co.sadad.noticeapi.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Schema(title = "آبجکت تک اعلان")
@Data
public class SendSingleNoticeReqDto implements Serializable {

    private static final long serialVersionUID = 7044673541516162886L;

    @Schema(title = "کد ملی کاربر")
    @NotNull(message = "ssn.is.required")
    private String ssn;

    @Schema(title = "مقدار تراکنش")
    @NotNull(message = "balance.is.required")
    private String balance;

    @Schema(title = "شماره حساب")
    @NotNull(message = "account.is.required")
    private String account;

    @Schema(title = "مانده موجودی")
    @NotNull(message = "withdraw.is.required")
    private String withdraw;

    @Schema(title = "نام بانک")
    private String bankName;

    @Schema(title = "تاریخ اعلان-epoch")
    @NotNull(message = "date.is.required")
    private Long date;
}
