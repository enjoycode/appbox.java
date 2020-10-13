package appbox.design.idea;

import appbox.design.DesignHub;
import appbox.logging.Log;
import appbox.store.ModelStore;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileSystem;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;

public final class ModelVirtualFile extends VirtualFile {
    public final  DesignHub designHub;
    private final boolean   isRoot;
    public final  long      modelId; //不直接引用Model实例

    /**
     * create for root
     */
    public ModelVirtualFile(DesignHub hub) {
        this.designHub = hub;
        this.modelId   = 0;
        isRoot         = true;
    }

    /**
     * create for model
     */
    public ModelVirtualFile(DesignHub hub, long modelId) {
        this.designHub = hub;
        this.modelId   = modelId;
        isRoot         = false;
    }

    //region ====VirtualFile====
    @Override
    public String getName() {
        return isRoot ? "/" : designHub.designTree.findModelNode(modelId).model().name();
    }

    @Override
    public VirtualFileSystem getFileSystem() {
        return designHub.typeSystem.workspace.virtualFileSystem;
    }

    @Override
    public String getPath() {
        return isRoot ? "/" : String.format("//%s", getName());
    }

    @Override
    public boolean isWritable() {
        return true;
    }

    @Override
    public boolean isDirectory() {
        return isRoot;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public VirtualFile getParent() {
        return isRoot ? null : designHub.typeSystem.workspace.virtualFileSystem.root;
    }

    @Override
    public VirtualFile[] getChildren() {
        if (!isDirectory()) {
            return new VirtualFile[0];
        }
        throw new RuntimeException("未实现"); //TODO:待实现
    }

    @Override
    public OutputStream getOutputStream(Object requestor, final long newModificationStamp, long newTimeStamp) throws IOException {
        if (isDirectory()) {
            return null;
        }
        throw new RuntimeException("未实现"); //TODO:待实现
    }

    @Override
    public byte[] contentsToByteArray() throws IOException {
        //TODO:判断当前节点是否签出，是则首先尝试从Staged中加载，再从ModelStore加载代码
        //TODO:直接从ModelStore加载bytes，不经过转换
        try {
            var res = ModelStore.loadServiceCodeAsync(modelId).get();
            Log.debug(String.format("Load service[%d] code from ModelStore.", modelId));
            return res.sourceCode.getBytes(StandardCharsets.UTF_8);
        } catch (Exception ex) {
            Log.warn("Can't load model's source code");
            return new byte[0];
        }
    }

    @Override
    public long getTimeStamp() {
        return isDirectory() ? 0 : designHub.designTree.findModelNode(modelId).model().version();
    }

    @Override
    public long getLength() {
        return 0; //TODO: check it
    }

    @Override
    public void refresh(boolean asynchronous, boolean recursive, Runnable postRunnable) {
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return VfsUtilCore.byteStreamSkippingBOM(contentsToByteArray(), this);
    }
    //endregion

    @Override
    public FileType getFileType() {
        return JavaFileType.INSTANCE;
    }

}
