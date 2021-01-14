package appbox.design.tree;

import appbox.design.common.CheckoutInfo;
import appbox.design.services.StagedService;
import appbox.model.ModelFolder;

import java.util.concurrent.CompletableFuture;

public final class FolderNode extends DesignNode {

    public final ModelFolder folder;

    public FolderNode(ModelFolder folder) {
        this.folder = folder;
        text        = folder.name();
    }

    @Override
    public DesignNodeType nodeType() {
        return DesignNodeType.FolderNode;
    }

    @Override
    public String id() {
        return folder.id().toString();
    }

    @Override
    public CheckoutInfo getCheckoutInfo() {
        //注意：返回相应的模型根节点的签出信息
        var rootNode = designTree().findModelRootNode(folder.appId(), folder.targetModelType());
        return rootNode.getCheckoutInfo();
    }

    @Override
    public int version() {
        return folder.getRoot().getVersion();
    }

    public void setVersion(int version) {
        folder.getRoot().setVersion(version);
    }

    public CompletableFuture<Void> saveAsync() {
        return StagedService.saveFolderAsync(folder.getRoot()); //保存的是根文件夹
    }

}
