package appbox.design.idea;

import appbox.design.DesignHub;
import com.intellij.openapi.vfs.DeprecatedVirtualFileSystem;
import com.intellij.openapi.vfs.NonPhysicalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;

import java.util.ArrayList;

/**
 * 每个DesignHub的TypeSystem的Workspace对应一个实例
 */
public class ModelVirtualFileSystem extends DeprecatedVirtualFileSystem implements NonPhysicalFileSystem {
    private static final String PROTOCOL = "model";
    private final DesignHub _designHub;
    public final ModelVirtualFile root;
    public final ArrayList<ModelVirtualFile> modelFiles = new ArrayList<>(); //暂在这里存储所有模型的虚拟文件

    public ModelVirtualFileSystem(DesignHub designHub) {
        _designHub = designHub;
        root = new ModelVirtualFile(designHub);
        this.startEventPropagation();
    }

    @Override
    public String getProtocol() {
        return ModelVirtualFileSystem.PROTOCOL;
    }

    @Override
    public VirtualFile findFileByPath(String path) {
        return null;
    }

    @Override
    public void refresh(boolean asynchronous) {
    }

    @Override
    public VirtualFile refreshAndFindFileByPath(String path) {
        return null;
    }

    /**
     * 根据模型标识找到对应的虚拟文件
     */
    public ModelVirtualFile findFileByModelId(long modelId) {
        return modelFiles.stream().filter(t -> t.modelId == modelId).findFirst().get();
    }
}
