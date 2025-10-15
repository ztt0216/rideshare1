package com.rideshare.util;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.LocalDateTime;

public class TimeZoneUtil {
    // Melbourne timezone
    public static final ZoneId MELBOURNE_ZONE = ZoneId.of("Australia/Melbourne");
    
    /**
     * Get current time in Melbourne timezone
     */
    public static ZonedDateTime now() {
        return ZonedDateTime.now(MELBOURNE_ZONE);
    }
    
    /**
     * Get current LocalDateTime in Melbourne timezone
     */
    public static LocalDateTime nowLocal() {
        return LocalDateTime.now(MELBOURNE_ZONE);
    }
}
