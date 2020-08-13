package appbox.core.utils;

public final class IdUtil {
    private IdUtil() {
    }

    public static final int MODELID_APPID_OFFSET = 32;

    public static final long MODELID_LAYER_MASK = 3;

    public static int getAppIdFromModelId(long modelId) {
        return (int) (modelId >>> MODELID_APPID_OFFSET);
    }
}
