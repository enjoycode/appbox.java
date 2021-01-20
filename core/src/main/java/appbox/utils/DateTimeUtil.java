package appbox.utils;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;

public final class DateTimeUtil {
    static final long NANOS_PER_SECOND = 1000_000_000L;

    private DateTimeUtil() {}

    public static LocalDateTime fromEpochMilli(long epochMilli) {
        long secs  = Math.floorDiv(epochMilli, 1000);
        int  nanos = Math.floorMod(epochMilli, 1000);
        return LocalDateTime.ofEpochSecond(secs, nanos * 1000_000, ZoneOffset.UTC);
    }

    public static long toEpochMilli(LocalDateTime localDateTime) {
        var  epochSecond    = localDateTime.toEpochSecond(ZoneOffset.UTC);
        var  nanoAdjustment = localDateTime.toLocalTime().getNano();
        long seconds        = Math.addExact(epochSecond, Math.floorDiv(nanoAdjustment, NANOS_PER_SECOND));
        int  nanos          = (int) Math.floorMod(nanoAdjustment, NANOS_PER_SECOND);

        if (seconds < 0 && nanos > 0) {
            long millis     = Math.multiplyExact(seconds + 1, 1000);
            long adjustment = nanos / 1000_000 - 1000;
            return Math.addExact(millis, adjustment);
        } else {
            long millis = Math.multiplyExact(seconds, 1000);
            return Math.addExact(millis, nanos / 1000_000);
        }
    }

    public static Date toDate(LocalDateTime localDateTime) {
        return new Date(toEpochMilli(localDateTime));
    }

}
