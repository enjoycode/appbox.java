package appbox.design.utils;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

public final class PathUtil {

    private static final IPath tmpdir = Path.forPosix(System.getProperty("java.io.tmpdir"));

    public static final IPath INDEX_DATA = tmpdir.append("appbox").append("index");

    public static IPath getWorkingLocation(long sessionId) {
        return tmpdir.append("appbox").append("workspace").append(Long.toUnsignedString(sessionId));
    }

}
