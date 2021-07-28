package appbox.compression;

import appbox.serialization.BytesOutputStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;

public final class GZipUtil {
    private GZipUtil() {}

    public static byte[] compressFileToBytes(File file) throws IOException {
        var fis = new FileInputStream(file);
        var fos = new BytesOutputStream(512);
        fos.writeByte(CompressType.GZip.value); //gzip compress flag
        var gzipOS = new GZIPOutputStream(fos);

        byte[] buffer = new byte[1024];
        int    len;
        while ((len = fis.read(buffer)) != -1) {
            gzipOS.write(buffer, 0, len);
        }
        //close resources
        gzipOS.close();
        fos.close();
        fis.close();

        return fos.toByteArray();
    }
}
