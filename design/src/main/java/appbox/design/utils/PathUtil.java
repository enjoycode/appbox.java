package appbox.design.utils;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

public final class PathUtil {

    public static final String TMP_PATH = System.getProperty("java.io.tmpdir");

    public static final String CURRENT_PATH = System.getProperty("user.dir");

    private static final IPath TMP_DIR = Path.forPosix(TMP_PATH);

    public static final IPath INDEX_DATA = TMP_DIR.append("appbox").append("index");

    public static final IPath WORKSPACE_PATH = TMP_DIR.append("appbox").append("workspace");

    /** 释放的第三方库路径 */
    public static final String LIB_PATH = java.nio.file.Path.of(TMP_PATH, "appbox", "lib").toString();

    public static java.nio.file.Path getDebugPath(String debugSessionId) {
        return java.nio.file.Path.of(TMP_PATH, "appbox", "debug", debugSessionId);
    }

}
