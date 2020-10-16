package appbox.logging;

//TODO: impl it

import java.time.LocalDateTime;

public final class Log {
    public static void debug(String msg) {
        log("\33[34m[D", msg);
    }

    public static void info(String msg) {
        log("\33[32m[I", msg);
    }

    public static void warn(String msg) {
        log("\33[33m[W", msg);
    }

    public static void error(String msg) {
        log("\33[31m[E", msg);
    }

    private static void log(String level, String msg) {
        var f = StackWalker.getInstance(/*StackWalker.Option.RETAIN_CLASS_REFERENCE*/)
                .walk(s -> s.skip(2).limit(1).findFirst())
                .get();
        System.out.format("%s%s %s:%d]:\33[0m %s\n", level, LocalDateTime.now().toString(),
                f.getFileName(), f.getLineNumber(), msg);
    }
}
