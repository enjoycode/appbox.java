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
import java.util.List;
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

    /** 释放服务模型的所有第三方包 */
    public static CompletableFuture<Void> extractService3rdLibs(String appName, List<String> deps) {
        CompletableFuture<Void> task = CompletableFuture.completedFuture(null);
        if (deps != null && deps.size() > 0) {
            for (var libName : deps) {
                final var asmName = String.format("%s/%s", appName, libName);
                task = task.thenCompose(r -> AssemblyUtil.extract3rdLib(asmName, false));
            }
        }
        return task;
    }

    /** 将应用依赖的第三方包释放至/tmp/appbox/lib/目录内 */
    public static CompletableFuture<Void> extract3rdLib(String asmName, boolean overrideExists) {
        final var libFilePath = LIB_PATH.resolve(asmName);

        //判断是否已存在
        if (!overrideExists && Files.exists(libFilePath)) {
            return CompletableFuture.completedFuture(null);
        }

        return ModelStore.loadAssemblyAsync(MetaAssemblyType.Application, asmName)
                .thenAccept(asmData -> {
                    var input = new ByteArrayInputStream(asmData);
                    //读压缩类型, TODO:检查类型或根据类型解压缩
                    var compressType = input.read();
                    try {
                        if (!Files.exists(libFilePath.getParent())) {
                            Files.createDirectories(libFilePath.getParent());
                        }

                        var libData = BrotliUtil.decompressFrom(input);
                        Files.write(libFilePath, libData,
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
