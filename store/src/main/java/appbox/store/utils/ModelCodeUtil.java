package appbox.store.utils;

import appbox.logging.Log;
import appbox.store.ServiceCode;
import com.nixxcode.jvmbrotli.common.BrotliLoader;
import com.nixxcode.jvmbrotli.dec.BrotliInputStream;
import com.nixxcode.jvmbrotli.enc.BrotliOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * 用于压缩编解码模型的代码
 */
public final class ModelCodeUtil {

    static {
        BrotliLoader.isBrotliAvailable();
    }

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
            var brotli = new BrotliOutputStream(out);
            brotli.write(utf8CodeData);
            brotli.close();
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
            var brotli   = new BrotliInputStream(input);
            var utf8data = brotli.readAllBytes();
            serviceCode.sourceCode = new String(utf8data, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

        return serviceCode;
    }

    /**
     * 使用gzip压缩字符串
     * @param code
     * @return
     */
    public static byte[] compressCode(String code) {
        if (code == null || code.length() == 0) {
            return new byte[0];
        }
        ByteArrayOutputStream out  = new ByteArrayOutputStream();
        GZIPOutputStream      gzip = null;
        try {
            gzip = new GZIPOutputStream(out);
            gzip.write(code.getBytes());
        } catch (IOException e) {
            Log.error("compressCode error");
        } finally {
            if (gzip != null) {
                try {
                    gzip.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return out.toByteArray();
    }

    /**
     * 使用gzip解压缩
     * @param compressed
     * @return
     */
    public static String decompressCode(byte[] compressed)
    {
        if (compressed == null||compressed.length==0) {
            return null;
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayInputStream in = null;
        GZIPInputStream ginzip = null;
        String decompressed = null;
        try {
            in = new ByteArrayInputStream(compressed);
            ginzip = new GZIPInputStream(in);
            byte[] buffer = new byte[1024];
            int offset = -1;
            while ((offset = ginzip.read(buffer)) != -1) {
                out.write(buffer, 0, offset);
            }
            decompressed = out.toString();
        } catch (IOException e) {
            Log.error("decompressCode error");
        } finally {
            if (ginzip != null) {
                try {
                    ginzip.close();
                } catch (IOException e) {
                }
            }
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                }
            }
        }
        return decompressed;
    }

    public static byte[] encodeViewCode(String templateCode, String scriptCode, String styleCode) {
        return null;//TODO
    }

    public static Map decodeViewCode(byte[] r) {
        return null;//TODO
    }

    public static byte[] encodeViewRuntimeCode(String runtimeCode) {
        return null;//TODO
    }

    public static String decodeViewRuntimeCode(byte[] data) {
        return null;
    }
}
