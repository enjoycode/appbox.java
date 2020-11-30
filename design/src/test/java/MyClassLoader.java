public class MyClassLoader extends ClassLoader{

    public MyClassLoader() {
    }
    public Class<?> defineClassPublic(String name, byte[] b, int off, int len) throws ClassFormatError {
        Class<?> clazz = defineClass(name, b, off, len);
        return clazz;
    }
}
