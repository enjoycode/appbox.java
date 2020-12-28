package appbox.server.runtime;

import appbox.compression.BrotliUtil;
import appbox.serialization.BytesInputStream;

import java.util.ArrayList;
import java.util.List;

/** 用于加载压缩过的服务字节码 */
public final class ServiceClassLoader extends ClassLoader {
    private static final class ClassInfo {
        public String name;
        public int    position;
        public int    length;
    }

    private byte[]       classData;  //仅暂存
    private ClassInfo[]  classes;    //仅暂存
    private List<String> hasLoaded; //仅暂存

    public Class<?> loadServiceClass(String name, byte[] compressedData) throws Exception {
        classData = BrotliUtil.decompress(compressedData);
        var      input        = new BytesInputStream(classData);
        Class<?> serviceClass = null;

        int count = input.readVariant();
        classes = new ClassInfo[count];
        //1. 先读取字节码信息
        for (int i = 0; i < count; i++) {
            var className = input.readString();
            var dataLen   = input.readVariant();
            var clazz     = new ClassInfo();
            clazz.name     = className;
            clazz.position = input.getPosition();
            clazz.length   = dataLen;
            classes[i]     = clazz;
            input.skip(dataLen);
        }

        //2. 加载字节码
        hasLoaded = new ArrayList<String>(); //防止重复定义
        for (var item : classes) {
            if (hasLoaded.stream().noneMatch(t -> t.equals(item.name))) {
                var clz = defineClass(item.name, classData, item.position, item.length);
                if (item.name.equals(name)) {
                    serviceClass = clz;
                } else {
                    hasLoaded.add(item.name);
                }
            }
        }

        //3. 清理暂存的信息
        classes   = null;
        classData = null;
        hasLoaded = null;

        return serviceClass;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        for (ClassInfo aClass : classes) {
            if (aClass.name.equals(name)) {
                hasLoaded.add(name);
                return defineClass(aClass.name, classData, aClass.position, aClass.length);
            }
        }
        return super.findClass(name);
    }
}
