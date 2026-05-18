package com.kangaroo.sparring.global.support;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

public final class KstDateTimeSupport {

    private static final ZoneId KST_ZONE_ID = ZoneId.of("Asia/Seoul");

    private KstDateTimeSupport() {
    }

    public static LocalDateTime nowDateTime() {
        return LocalDateTime.now(KST_ZONE_ID);
    }

    public static LocalDate nowDate() {
        return LocalDate.now(KST_ZONE_ID);
    }

    public static ZoneId zoneId() {
        return KST_ZONE_ID;
    }
}
