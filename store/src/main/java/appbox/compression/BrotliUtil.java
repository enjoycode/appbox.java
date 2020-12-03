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
        var brotli = new BrotliOutputStream(out);
        brotli.write(src);
        brotli.close();
    }

    public static byte[] compress(byte[] src) throws IOException {
        var outputStream = new ByteArrayOutputStream();
        compressTo(src, outputStream);
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
