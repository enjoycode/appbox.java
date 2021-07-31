package appbox.server.runtime;

import appbox.compression.BrotliUtil;
import appbox.logging.Log;
import appbox.serialization.BytesInputStream;
import appbox.store.utils.AssemblyUtil;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/** 用于加载压缩过的服务字节码 */
public final class ServiceClassLoader extends ClassLoader {

    private JarLoader[] jarLoaders;  //用于从jar包中加载类

    private byte[]       classData;  //仅暂存
    private ClassInfo[]  classes;    //仅暂存
    private List<String> hasLoaded;  //仅暂存

    /** 从压缩的字节码中解析服务类 */
    public Class<?> loadServiceClass(String name, byte[] compressedData) throws Exception {
        classData = BrotliUtil.decompress(compressedData);
        var      input        = new BytesInputStream(classData);
        Class<?> serviceClass = null;

        //格式参考PublishService.compileService()
        int count = input.readVariant();
        classes = new ClassInfo[count];
        //1. 先读取字节码信息及相关第三方包引用
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
        if (input.hasRemaining()) {
            final var deps = input.readListString();
            //释放第三方包,可能已经存在
            jarLoaders = new JarLoader[deps.size()];
            for (int i = 0; i < deps.size(); i++) {
                AssemblyUtil.extract3rdLib(deps.get(i), false).join(); //TODO:暂转为同步
                jarLoaders[i] = new JarLoader(AssemblyUtil.LIB_PATH.resolve(deps.get(i)));
            }
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
    protected String findLibrary(String libname) {
        Log.warn("Find library: " + libname);
        return super.findLibrary(libname);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        //先查找系统类
        if (classes != null) { //在加载完服务类型信息后会置空
            for (ClassInfo aClass : classes) {
                if (aClass.name.equals(name)) {
                    hasLoaded.add(name);
                    return defineClass(aClass.name, classData, aClass.position, aClass.length);
                }
            }
        }

        //再查找第三方包 TODO:暂简单实现从第三方jar包加载类
        Log.debug("Find 3rd lib class: " + name);
        if (jarLoaders != null && jarLoaders.length > 0) {
            final var classPath = name.replace('.', '/').concat(".class");
            for (int i = 0; i < jarLoaders.length; i++) {
                final var classData = jarLoaders[i].findClassData(classPath);
                if (classData != null)
                    return defineClass(name, classData, 0, classData.length);
            }
        }

        return super.findClass(name);
    }

    private static final class ClassInfo {
        public String name;
        public int    position;
        public int    length;
    }

    /** 用于从第三方jar包中加载类 */
    private static final class JarLoader {
        private final JarFile jarFile;

        public JarLoader(Path jarFilePath) throws IOException {
            jarFile = new JarFile(jarFilePath.toFile(), true, 1, JarFile.runtimeVersion());
        }

        public byte[] findClassData(String classPath) {
            final JarEntry entry = jarFile.getJarEntry(classPath);
            if (entry == null)
                return null;

            try {
                final var input = jarFile.getInputStream(entry);
                final var data  = input.readAllBytes();
                input.close();
                return data;
            } catch (IOException e) {
                Log.warn("Can't find class: " + classPath);
                return null;
            }
        }
    }

}
