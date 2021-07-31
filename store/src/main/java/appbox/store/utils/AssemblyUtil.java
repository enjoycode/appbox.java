package appbox.store.utils;

import appbox.compression.BrotliUtil;
import appbox.compression.CompressType;
import appbox.logging.Log;
import appbox.serialization.BytesOutputStream;
import appbox.store.KVTransaction;
import appbox.store.MetaAssemblyType;
import appbox.store.ModelStore;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.CompletableFuture;

public final class AssemblyUtil {
    public static final Path LIB_PATH = Path.of(System.getProperty("java.io.tmpdir"), "appbox", "lib");

    static {
        try {
            if (!Files.exists(LIB_PATH)) {
                Files.createDirectories(LIB_PATH);
            }
        } catch (Exception ex) {
            Log.error("Can't create lib path.");
        }
    }

    private AssemblyUtil() {}

    /** 将上传的第三方包保存至ModelStore内 */
    public static CompletableFuture<Void> save3rdLibToModelStore(Path tmpFile, String path) {
        final var out = new BytesOutputStream(2048);
        out.write(CompressType.Brotli.value); //写入1字节压缩类型标记
        try {
            try (var compress = BrotliUtil.makeCompressStream(out)) {
                Files.copy(tmpFile, compress);
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        final var bytes = out.toByteArray();

        //save to ModelStore
        return KVTransaction.beginAsync()
                .thenCompose(txn -> ModelStore.upsertAssemblyAsync(MetaAssemblyType.Application, path, bytes, txn)
                        .thenCompose(r -> txn.commitAsync()));
    }

    /** 将应用依赖的第三方包释放至/tmp/appbox/lib/目录内 */
    public static CompletableFuture<Void> extract3rdLib(String appName, String libName) {
        //TODO:考虑判断是否已存在
        final String asmName = appName + "/" + libName;
        return ModelStore.loadAssemblyAsync(MetaAssemblyType.Application, asmName)
                .thenAccept(asmData -> {
                    var input = new ByteArrayInputStream(asmData);
                    //读压缩类型, TODO:检查类型或根据类型解压缩
                    var compressType = input.read();

                    try {
                        if (!Files.exists(LIB_PATH)) {
                            Files.createDirectories(LIB_PATH);
                        }

                        var libData = BrotliUtil.decompressFrom(input);
                        Files.write(java.nio.file.Path.of(LIB_PATH.toString(), libName), libData,
                                StandardOpenOption.CREATE,
                                StandardOpenOption.WRITE,
                                StandardOpenOption.TRUNCATE_EXISTING);
                    } catch (Exception ex) {
                        Log.error("Can't extract 3rd lib: " + asmName);
                        throw new RuntimeException(ex);
                    }
                });
    }
}
