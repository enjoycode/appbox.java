package appbox.design.utils;

public final class ReflectUtil {

    private ReflectUtil() {}

    public static <T> void setField(Class<T> clz, String fieldName, Object instance, Object newValue)
            throws NoSuchFieldException, IllegalAccessException {
        var field = clz.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(instance, newValue);
        field.setAccessible(false);
    }

    public static Object getField(Class<?> clz, String fieldName, Object instance) {
        try {
            var field = clz.getDeclaredField(fieldName);
            field.setAccessible(true);
            var value = field.get(instance);
            field.setAccessible(false);
            return value;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static void invokeMethod(Class<?> clazz, String methodName, Object instance) {
        try {
            var method = clazz.getDeclaredMethod(methodName);
            method.setAccessible(true);
            method.invoke(instance);
            method.setAccessible(false);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static void invokeMethod(Class<?> clazz, String methodName,
                                    Class<?> p1Clz, Class<?> p2Clz,
                                    Object instance, Object p1, Object p2) {
        try {
            var method = clazz.getDeclaredMethod(methodName, p1Clz, p2Clz);
            method.setAccessible(true);
            method.invoke(instance, p1, p2);
            method.setAccessible(false);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

}
