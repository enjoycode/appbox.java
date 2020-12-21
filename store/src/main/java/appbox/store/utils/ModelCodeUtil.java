package appbox.store.utils;

import appbox.compression.BrotliUtil;
import appbox.serialization.BytesInputStream;
import appbox.serialization.BytesOutputStream;
import appbox.store.ServiceCode;
import appbox.store.ViewCode;

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

    public static byte[] encodeViewCode(String templateCode, String scriptCode, String styleCode) {
        var out = new BytesOutputStream(2048);
        //写入1字节压缩类型标记
        out.write(1);
        var    templateCodeBytes = templateCode.getBytes(StandardCharsets.UTF_8);
        var    scriptCodeBytes   = scriptCode.getBytes(StandardCharsets.UTF_8);
        byte[] styleCodeBytes    = null; //可能为空
        if (styleCode != null && styleCode.length() > 0)
            styleCodeBytes = styleCode.getBytes(StandardCharsets.UTF_8);
        //暂不同C#版本（写入字符数）
        out.writeVariant(templateCodeBytes.length);
        out.writeVariant(scriptCodeBytes.length);
        out.writeVariant(styleCodeBytes != null ? styleCodeBytes.length : 0);
        //再写入压缩的utf8
        try {
            try(var compress = BrotliUtil.makeCompressStream(out)) {
                compress.write(templateCodeBytes);
                compress.write(scriptCodeBytes);
                if (styleCodeBytes != null)
                    compress.write(styleCodeBytes);
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

        return out.toByteArray();
    }

    public static byte[] encodeViewRuntimeCode(String runtimeCode) {
        var utf8data = runtimeCode.getBytes(StandardCharsets.UTF_8);
        var out      = new ByteArrayOutputStream();
        //写入1字节压缩类型标记
        out.write(1);
        //再写入压缩的utf8
        try {
            BrotliUtil.compressTo(utf8data, out);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        return out.toByteArray();
    }

    public static ViewCode decodeViewCode(byte[] data) {
        var viewCode = new ViewCode();

        var input = new BytesInputStream(data);
        //读压缩类型
        input.readByte();
        //读取utf8编码后的长度
        var templateCodeLen = input.readVariant();
        var scriptCodeLen = input.readVariant();
        var styleCodeLen = input.readVariant();
        try {
            try (var decompress = BrotliUtil.makeDecompressStream(input)) {
                byte[] utf8data = decompress.readNBytes(templateCodeLen);
                viewCode.Template = new String(utf8data, StandardCharsets.UTF_8);
                utf8data = decompress.readNBytes(scriptCodeLen);
                viewCode.Script = new String(utf8data, StandardCharsets.UTF_8);
                if (styleCodeLen > 0){
                    utf8data = decompress.readNBytes(styleCodeLen);
                    viewCode.Style = new String(utf8data, StandardCharsets.UTF_8);
                }
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

        return viewCode;
    }

}
