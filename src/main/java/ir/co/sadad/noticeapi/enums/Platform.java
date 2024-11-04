package ir.co.sadad.noticeapi.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Platform {

    ANDROID("ANDROID"),
    IOS("IOS"),
    PWA("PWA"),

    ALL("ALL"),
    ANDROID_TEST("ANDROID_TEST");

    final String desc;
}
