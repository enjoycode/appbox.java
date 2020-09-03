package appbox.store.utils;

import com.nixxcode.jvmbrotli.common.BrotliLoader;
import com.nixxcode.jvmbrotli.dec.BrotliInputStream;
import com.nixxcode.jvmbrotli.enc.BrotliOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 用于压缩编解码模型的代码
 */
public final class ModelCodeUtil {
    public static final class ServiceCode {
        public String  sourceCode;
        public boolean isDeclare;
    }


    static {
        BrotliLoader.isBrotliAvailable();
    }

    private ModelCodeUtil() {
    }

    /**
     * 压缩编码服务代码
     *
     * @param isDeclare 是否仅声明代码(无实现)
     */
    public static byte[] encodeServiceCode(String sourceCode, boolean isDeclare) throws IOException {
        //TODO:判断少量代码不压缩
        var utf8data = sourceCode.getBytes(StandardCharsets.UTF_8);
        var out      = new ByteArrayOutputStream();
        //写入1字节压缩类型标记
        out.write(1);
        //写入1字节是否声明代码
        out.write(isDeclare ? 1 : 0);

        //再写入压缩的utf8
        var brotli = new BrotliOutputStream(out);
        brotli.write(utf8data);
        brotli.close();

        return out.toByteArray();
    }

    public static ServiceCode decodeServiceCode(byte[] data) throws IOException {
        var serviceCode = new ServiceCode();

        var input = new ByteArrayInputStream(data);
        //读压缩类型, TODO:检查类型或根据类型解压缩
        var compressType = input.read();
        serviceCode.isDeclare = input.read() == 1 ? true : false;

        var brotli   = new BrotliInputStream(input);
        var utf8data = brotli.readAllBytes();
        serviceCode.sourceCode = new String(utf8data, StandardCharsets.UTF_8);
        return serviceCode;
    }

}
