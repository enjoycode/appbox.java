package appbox.design.services.code;

import appbox.design.DesignHub;
import appbox.design.idea.IdeaApplicationEnvironment;
import appbox.design.idea.IdeaProjectEnvironment;
import appbox.design.idea.ModelVirtualFile;
import appbox.design.idea.ModelVirtualFileSystem;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;

import java.util.HashMap;

/**
 * 一个TypeSystem对应一个Workspace实例，管理JavaProject及相应的虚拟文件
 */
public final class Workspace {
    private final IdeaProjectEnvironment          projectEnvironment;
    private final HashMap<Long, ModelVirtualFile> openedFiles = new HashMap<>();
    public final  ModelVirtualFileSystem          virtualFileSystem;

    public Workspace(DesignHub designHub) {
        projectEnvironment = new IdeaProjectEnvironment(IdeaApplicationEnvironment.INSTANCE);
        virtualFileSystem  = new ModelVirtualFileSystem(designHub);
    }

    public Document openDocument(long modelId) {
        //TODO:仅允许打开特定类型的
        var file = virtualFileSystem.findFileByModelId(modelId);
        openedFiles.put(modelId, file);
        return FileDocumentManager.getInstance().getDocument(file);
    }

}
