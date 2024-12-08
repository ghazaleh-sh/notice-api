package ir.co.sadad.noticeapi.enums;

import lombok.Getter;

@Getter
public enum NoticeType {
    TRANSACTION("transaction"),
    CAMPAIGN("campaign"),
    GENERAL("general");

    private final String value;

    NoticeType(String value) {
        this.value = value;
    }
}