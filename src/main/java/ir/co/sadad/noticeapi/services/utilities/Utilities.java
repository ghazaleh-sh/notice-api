package ir.co.sadad.noticeapi.services.utilities;

import ir.co.sadad.noticeapi.enums.Platform;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class Utilities {

    public static String currentUTCDate() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        return formatter.format(Instant.now().atZone(ZoneId.of("UTC")));
    }

    public static Platform checkUserAgent(String userAgent) {
        if (userAgent.contains("CxpMobileAndroid"))
            return Platform.ANDROID;
        if (userAgent.contains("CxpMobileiOS"))
            return Platform.IOS;
        if (userAgent.contains("BaamBaseGateway/1.0 && Android") ||
                userAgent.contains("BaamBaseGateway/1.0 && iPhone") ||
                userAgent.contains("BaamBaseGateway/1.0 && iPad"))
            return Platform.PWA;
        else return null;
    }
}
