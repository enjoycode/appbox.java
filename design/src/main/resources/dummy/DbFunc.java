import sys.*;

import java.util.Collection;

@RuntimeType(type = "appbox.store.DbFunc")
public final class DbFunc {

    private DbFunc() {}

    public static int sum(int field) {return 0;}

    public static long sum(long field) {return 0;}

    public static int max(int field) { return 0;}

    public static long max(long field) { return 0L; }

    public static <T> boolean in(T field, Collection<T> collection) { return false; }

    public static <T> boolean notIN(T field, Collection<T> collection) { return false; }

}
