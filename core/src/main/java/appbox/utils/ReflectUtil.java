package appbox.utils;

import sun.misc.Unsafe;

import java.lang.reflect.*;
import java.util.Arrays;

public final class ReflectUtil {
    private ReflectUtil() {
    }

    public static Class<?> getRawType(Type type) {
        if (type instanceof Class) {
            return (Class) type;
        } else if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            Type rawType = parameterizedType.getRawType();
            return (Class) rawType;
        } else if (type instanceof GenericArrayType) {
            Type componentType = ((GenericArrayType) type).getGenericComponentType();
            return Array.newInstance(getRawType(componentType), 0).getClass();
        } else if (type instanceof TypeVariable) {
            return Object.class;
        } else if (type instanceof WildcardType) {
            return getRawType(((WildcardType) type).getUpperBounds()[0]);
        } else {
            String className = type == null ? "null" : type.getClass().getName();
            throw new IllegalArgumentException("Expected a Class, ParameterizedType, or GenericArrayType, but <" + type + "> is of type " + className);
        }
    }

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
