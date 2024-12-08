package ir.co.sadad.noticeapi.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum NotificationStatus {

    ACTIVE("ACTIVE"),
    INACTIVE("INACTIVE");

    private String value;
}
