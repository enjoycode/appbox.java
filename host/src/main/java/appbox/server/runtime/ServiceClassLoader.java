package appbox.server.runtime;

import appbox.compression.BrotliUtil;
import appbox.serialization.BytesInputStream;

import java.io.IOException;

/** 用于加载压缩过的服务字节码 */
public final class ServiceClassLoader extends ClassLoader {

    public Class<?> loadServiceClass(String name, byte[] compressedData) throws IOException {
        var      data         = BrotliUtil.decompress(compressedData);
        var      input        = new BytesInputStream(data);
        Class<?> serviceClass = null;

        int count = input.readVariant();
        for (int i = 0; i < count; i++) {
            var className = input.readString();
            var dataLen   = input.readVariant();
            var clazz     = defineClass(className, data, input.getPosition(), dataLen);
            input.skip(dataLen);
            if (className.equals(name)) {
                serviceClass = clazz;
            }
        }

        return serviceClass;
    }

}
