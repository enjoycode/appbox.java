package appbox.server.runtime;

import appbox.compression.BrotliUtil;
import appbox.serialization.BinDeserializer;
import appbox.serialization.BytesInputStream;

import java.io.IOException;

/** 用于加载压缩过的服务字节码 */
public final class ServiceClassLoader extends ClassLoader {

    public Class<?> loadServiceClass(String name, byte[] compressedData) throws IOException {
        var      data         = BrotliUtil.decompress(compressedData);
        var      input        = new BytesInputStream(data);
        var      ds           = BinDeserializer.rentFromPool(input);
        Class<?> serviceClass = null;
        try {
            int count = ds.readVariant();
            for (int i = 0; i < count; i++) {
                var className = ds.readString();
                var dataLen   = ds.readVariant();
                var clazz     = defineClass(className, data, input.getPosition(), dataLen);
                ds.skip(dataLen);
                if (className.equals(name)) {
                    serviceClass = clazz;
                }
            }
        } finally {
            BinDeserializer.backToPool(ds);
        }

        return serviceClass;
    }

}
