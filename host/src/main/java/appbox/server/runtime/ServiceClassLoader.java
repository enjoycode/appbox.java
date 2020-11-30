package appbox.server.runtime;

import appbox.compression.BrotliUtil;

import java.io.IOException;

/** 用于加载压缩过的服务字节码 */
public final class ServiceClassLoader extends ClassLoader {

    public Class<?> loadServiceClass(String name, byte[] compressedData) throws IOException {
        //先解压
        var classData = BrotliUtil.decompress(compressedData);
        //再加载
        return defineClass(name, classData, 0, classData.length);
    }

}
