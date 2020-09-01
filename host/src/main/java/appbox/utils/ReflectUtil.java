package appbox.utils;

import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;

public final class ReflectUtil {

    private ReflectUtil(){}

    /**
     * 反射设置私有final静态成员的值
     */
    public static void setFinalStatic(Field field, Object newValue) throws Exception {
        //disable illegal-access=warn
        try {
            Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            Unsafe u = (Unsafe) theUnsafe.get(null);

            Class cls = Class.forName("jdk.internal.module.IllegalAccessLogger");
            Field logger = cls.getDeclaredField("logger");
            u.putObjectVolatile(cls, u.staticFieldOffset(logger), null);
        } catch (Exception e) {
            // ignore
        }

        field.setAccessible(true);

        var methods = Field.class.getClass().getDeclaredMethods();
        var method = Arrays.stream(methods).filter(m -> m.getName().equals("getDeclaredFields0")).findFirst().get();
        method.setAccessible(true);
        var fields = (Field[]) method.invoke(field.getClass(), false);
        var modifiersField = Arrays.stream(fields).filter(f -> f.getName().equals("modifiers")).findFirst().get();

        //Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);

        field.set(null, newValue);

        field.setAccessible(false);
        modifiersField.setAccessible(false);
        method.setAccessible(false);
    }
}
