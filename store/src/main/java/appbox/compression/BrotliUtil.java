package appbox.compression;

import com.nixxcode.jvmbrotli.common.BrotliLoader;
import com.nixxcode.jvmbrotli.dec.BrotliInputStream;
import com.nixxcode.jvmbrotli.enc.BrotliOutputStream;

import java.io.*;

public final class BrotliUtil {

    static {
        BrotliLoader.isBrotliAvailable();
    }

    private BrotliUtil() {}

    public static void compressTo(byte[] src, OutputStream out) throws IOException {
        compressTo(src, 0, src.length, out);
    }

    public static void compressTo(byte[] src, int offset, int count, OutputStream out) throws IOException {
        var brotli = new BrotliOutputStream(out);
        brotli.write(src, offset, count);
        brotli.close();
    }

    public static byte[] compress(byte[] src) throws IOException {
        return compress(src, 0, src.length);
    }

    public static byte[] compress(byte[] src, int offset, int count) throws IOException {
        var outputStream = new ByteArrayOutputStream();
        compressTo(src, offset, count, outputStream);
        return outputStream.toByteArray();
    }

    public static byte[] decompressFrom(InputStream inputStream) throws IOException {
        var brotli = new BrotliInputStream(inputStream);
        var res    = brotli.readAllBytes();
        brotli.close();
        return res;
    }

    public static byte[] decompress(byte[] src) throws IOException {
        var inputStream = new ByteArrayInputStream(src);
        return decompressFrom(inputStream);
    }

}
