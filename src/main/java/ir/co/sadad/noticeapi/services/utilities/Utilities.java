package ir.co.sadad.noticeapi.services.utilities;

import ir.co.sadad.hambaam.persiandatetime.PersianUTC;
import ir.co.sadad.noticeapi.enums.Platform;

public class Utilities {

    public static String getCurrentUTC() {
        return PersianUTC.currentUTC().toString();
    }

    public static String getCurrentUTCDate() {
        return PersianUTC.currentUTC().getDate();
    }

    public static Platform checkUserAgent(String userAgent) {
        if (userAgent.contains("BaamBaseGateway"))
            return Platform.PWA;
        if (userAgent.contains("CxpMobileAndroid") && !(userAgent.contains("develop") || userAgent.contains("staging")))
            return Platform.ANDROID;
        if (userAgent.contains("CxpMobileiOS"))
            return Platform.IOS;
        if (userAgent.contains("CxpMobileAndroid") && (userAgent.contains("develop") || userAgent.contains("staging")))
            return Platform.ANDROID_TEST;
        else return null;
    }
}
