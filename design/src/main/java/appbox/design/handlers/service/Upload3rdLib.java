package appbox.design.handlers.service;

import appbox.design.DesignHub;
import appbox.design.handlers.IDesignHandler;
import appbox.runtime.InvokeArgs;
import appbox.store.utils.AssemblyUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/** 保存上传的第三方类库至ModelStore */
public final class Upload3rdLib implements IDesignHandler {

    @Override
    public CompletableFuture<Object> handle(DesignHub hub, InvokeArgs args) {
        final var temp    = args.getString();
        final var path    = args.getString();
        final var index   = path.indexOf('/');
        final var appName = path.substring(0, index);

        final var appNode = hub.designTree.findApplicationNodeByName(appName);
        if (appNode == null)
            return CompletableFuture.failedFuture(new RuntimeException("Can't find app: " + appName));

        final var filePath = Path.of(temp);
        if (!Files.exists(filePath))
            return CompletableFuture.failedFuture(new RuntimeException("Upload file not exists."));

        //compress and save to ModelStore
        return AssemblyUtil.save3rdLibToModelStore(filePath, path)
                .thenApply(r -> true);

        //TODO:*****
        // 1. 通知所有DesignHub.TypeSystem更新MetadataReference缓存，并更新相关项目引用
        //TypeSystem.RemoveMetadataReference(fileName, appID);
        // 2. 如果相应的AppContainer已启动，通知其移除所有引用该第三方组件的服务实例缓存，使其自动重新加载
        // 3. 如果集群模式，通知集群其他节点做上述1，2操作
    }

}
