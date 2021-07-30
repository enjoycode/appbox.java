package appbox.design.handlers.tool;

import appbox.design.DesignHub;
import appbox.design.handlers.IDesignHandler;
import appbox.design.tree.DesignNodeType;
import appbox.design.tree.ModelNode;
import appbox.design.utils.PathUtil;
import appbox.model.ModelType;
import appbox.runtime.InvokeArgs;
import appbox.store.MetaAssemblyType;
import appbox.store.ModelStore;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/** 用于将编码好的服务组件及视图组件保存为临时文件(仅用于生成初始化的系统模型) */
public final class GetAssembly implements IDesignHandler {

    @Override
    public CompletableFuture<Object> handle(DesignHub hub, InvokeArgs args) {
        var nodeType = DesignNodeType.fromValue((byte) args.getInt());
        var nodeId   = args.getString();

        var node = hub.designTree.findNode(nodeType, nodeId);
        if (node == null)
            return CompletableFuture.failedFuture(new RuntimeException("Can't find node"));
        if (!(node instanceof ModelNode))
            return CompletableFuture.failedFuture(new RuntimeException("Only Service or View node"));
        var modelNode = (ModelNode) node;
        if (modelNode.model().modelType() == ModelType.Service) {
            var asmName = modelNode.appNode.model.name() + "." + modelNode.model().name();
            return ModelStore.loadAssemblyAsync(MetaAssemblyType.Service, asmName).thenApply(data -> {
                if (data == null)
                    throw new RuntimeException("Can't load assembly");

                saveAssembly(data, asmName);
                return null;
            });
        } else if (modelNode.model().modelType() == ModelType.View) {
            var asmName = modelNode.appNode.model.name() + "." + modelNode.model().name();
            return ModelStore.loadAssemblyAsync(MetaAssemblyType.View, asmName).thenApply(data -> {
                if (data == null)
                    throw new RuntimeException("Can't load assembly");

                saveAssembly(data, asmName);
                return null;
            });
        } else {
            return CompletableFuture.failedFuture(new RuntimeException("Only Service or View node"));
        }
    }

    private static void saveAssembly(byte[] data, String asmName) {
        try {
            var filePath = Path.of(PathUtil.TMP_PATH, asmName + ".bin");
            Files.deleteIfExists(filePath);
            Files.write(filePath, data);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

}
