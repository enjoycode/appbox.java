package appbox.design.utils;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

public final class PathUtil {

    public static final String tmpPath = System.getProperty("java.io.tmpdir");

    private static final IPath tmpdir = Path.forPosix(tmpPath);

    public static final IPath INDEX_DATA = tmpdir.append("appbox").append("index");

    public static IPath getWorkingLocation(long sessionId) {
        return tmpdir.append("appbox").append("workspace").append(Long.toUnsignedString(sessionId));
    }

    public static java.nio.file.Path getDebugPath(String debugSessionId) {
        return java.nio.file.Path.of(tmpPath, "appbox", "debug", debugSessionId);
    }

}
