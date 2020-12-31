import sys.*;

import java.util.UUID;
import java.util.Collection;

@RuntimeType(type = "appbox.store.DbFunc")
public final class DbFunc {

    private DbFunc() {}

    public static <T extends Number> T sum(T field) {return null;}

    public static <T extends Number> T avg(T field) {return null;}

    public static <T extends Number> T max(T field) {return null;}

    public static <T extends Number> T min(T field) {return null;}

    public static boolean in(int field, SqlSubQuery<Integer> subQuery) { return false; }

    public static boolean in(long field, SqlSubQuery<Long> subQuery) { return false; }

    public static boolean in(String field, SqlSubQuery<String> subQuery) { return false; }

    public static boolean in(UUID field, SqlSubQuery<UUID> subQuery) { return false; }

}
