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

    public static Object getField(Class<?> clz, String fieldName, Object instance)
            throws NoSuchFieldException, IllegalAccessException {
        var field = clz.getDeclaredField(fieldName);
        field.setAccessible(true);
        var value = field.get(instance);
        field.setAccessible(false);
        return value;
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

}
