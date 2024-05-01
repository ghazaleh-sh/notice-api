package ir.co.sadad.noticeapi.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Platform {

    ANDROID("ANDROID"),
    IOS("IOS"),
    PWA("PWA");

    final String desc;
}
