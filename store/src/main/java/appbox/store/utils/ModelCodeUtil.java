package appbox.store.utils;

import appbox.compression.BrotliUtil;
import appbox.store.ServiceCode;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * 用于压缩编解码模型的代码
 */
public final class ModelCodeUtil {

    private ModelCodeUtil() {}

    /**
     * 压缩编码服务代码
     * @param isDeclare 是否仅声明代码(无实现)
     */
    public static byte[] encodeServiceCode(String sourceCode, boolean isDeclare) {
        //TODO:判断少量代码不压缩
        var utf8data = sourceCode.getBytes(StandardCharsets.UTF_8);
        return encodeServiceCodeData(utf8data, isDeclare);
    }

    public static byte[] encodeServiceCodeData(byte[] utf8CodeData, boolean isDeclare) {
        var out = new ByteArrayOutputStream();
        //写入1字节压缩类型标记
        out.write(1);
        //写入1字节是否声明代码
        out.write(isDeclare ? 1 : 0);

        //再写入压缩的utf8
        try {
            BrotliUtil.compressTo(utf8CodeData, out);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

        return out.toByteArray();
    }

    public static ServiceCode decodeServiceCode(byte[] data) {
        var serviceCode = new ServiceCode();

        var input = new ByteArrayInputStream(data);
        //读压缩类型, TODO:检查类型或根据类型解压缩
        var compressType = input.read();
        serviceCode.isDeclare = input.read() == 1;

        try {
            var utf8data = BrotliUtil.decompressFrom(input);
            serviceCode.sourceCode = new String(utf8data, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

        return serviceCode;
    }

}
