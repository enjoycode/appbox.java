import appbox.utils.DateTimeUtil;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

public class TestJava {

    @Test
    public void testDateTime() {
        var time1 = LocalDateTime.now();
        long secs = time1.toEpochSecond(ZoneOffset.UTC);
        var ms = time1.toInstant(ZoneOffset.UTC).toEpochMilli();
        var instant = Instant.ofEpochMilli(ms);
        var local = instant.atZone(ZoneId.systemDefault()).toLocalDateTime();

        var time2 = LocalDateTime.ofEpochSecond(secs, time1.getNano(), ZoneOffset.UTC);
        assertEquals(time1, time2);
    }

    @Test
    public void testDateTimeUtil() {
        var time1 = LocalDateTime.now();
        long mm = DateTimeUtil.toEpochMilli(time1);
        var time2 = DateTimeUtil.fromEpochMilli(mm);
    }

    @Test
    public void testReflection() {
        Object v1   = 1;
        var    type = v1.getClass();
        assertNotNull(type);

        String a = "Hello";
        String b = a;
        b += " Future";
        assertEquals("Hello", a);
    }

    @Test
    public void testCharSequence() {
        //var s0 = "Hello";
        //String.class.getConstructor()
        var s1  = "中Hello World";
        var s2  = "中Hello Future";
        var sq1 = s1.subSequence(0, 5);
        var sq2 = s2.subSequence(0, 5);
        assertEquals(sq1, sq2);
        assertEquals(sq1.hashCode(), sq2.hashCode());

        char[] arr = {'r', 'u', 'n', 'o', 'o', 'b'};
        var    s3  = String.valueOf(arr);
    }

    @Test
    void testUnsigned() {
        int  a  = -12345;
        long b  = -12345678L;
        var  a1 = Integer.toUnsignedString(a);
        var  b1 = Long.toUnsignedString(b);
        assertEquals(a, Integer.parseUnsignedInt(a1));
        assertEquals(b, Long.parseUnsignedLong(b1));
    }

    @Test
    void testCast() {
        var sb = new StringBuffer("a");
        sb.replace(0, 0, "Hello");
        assertEquals("Helloa", sb.toString());

        sb.replace(0, 5, "");
        assertEquals("a", sb.toString());
    }

    private CompletableFuture<Void> insertTask() {
        CompletableFuture<Void> future =
                CompletableFuture.failedFuture(new RuntimeException("OOPS!"));
        return future.whenComplete((r, ex) -> {
            if (ex != null) {
                System.out.println(ex.getMessage());
            }
        });
    }

    @Test
    void testFutureException() {
        try {
            insertTask().get();
        } catch (Exception ex) {
            System.out.println("任务失败: " + ex.getMessage());
        }
    }

    @Test
    void testSwitchCharSeq() {
        CharSequence name = "Rick";
        switch (name.toString()) {
            case "Rick":
                System.out.println("Hit"); break;
            default:
                System.out.println("not"); break;
        }
    }

}
