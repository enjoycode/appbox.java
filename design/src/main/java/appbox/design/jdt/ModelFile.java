package appbox.design.jdt;

import appbox.design.IDeveloperSession;
import appbox.logging.Log;
import appbox.runtime.RuntimeContext;
import appbox.store.ModelStore;
import org.eclipse.core.internal.resources.ResourceStatus;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFileState;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.content.IContentDescription;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

//注意：ModelFile不会指向同一实例

public final class ModelFile extends ModelResource implements IFile {

    private static final Map<IPath, InputStream> contentMap = new HashMap<>(); //TODO:待移除，测试用

    public ModelFile(IPath path, ModelWorkspace workspace) {
        super(path, workspace);
    }

    @Override
    public void appendContents(InputStream inputStream, boolean b, boolean b1, IProgressMonitor iProgressMonitor) throws CoreException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void appendContents(InputStream inputStream, int i, IProgressMonitor iProgressMonitor) throws CoreException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void create(InputStream content, boolean force, IProgressMonitor monitor) throws CoreException {
        create(content, (force ? IResource.FORCE : IResource.NONE), monitor);
    }

    @Override
    public void create(InputStream content, int updateFlags, IProgressMonitor monitor) throws CoreException {
        //TODO:
        var     info  = workspace.createResource(this, updateFlags);
        boolean local = content != null;
        contentMap.put(this.path, content);
        if (!local)
            getResourceInfo(true, true).clearModificationStamp();
    }

    @Override
    public String getCharset() throws CoreException {
        return null;
    }

    @Override
    public String getCharset(boolean b) throws CoreException {
        return null;
    }

    @Override
    public String getCharsetFor(Reader reader) throws CoreException {
        return null;
    }

    @Override
    public IContentDescription getContentDescription() throws CoreException {
        throw new UnsupportedOperationException();
    }

    @Override
    public InputStream getContents() throws CoreException {
        return getContents(true);
    }

    @Override
    public InputStream getContents(boolean force) throws CoreException {
        //TODO:判断当前节点是否签出，是则首先尝试从Staged中加载，再从ModelStore加载代码
        try {
            var session   = (IDeveloperSession) RuntimeContext.current().currentSession();
            var hub       = session.getDesignHub();
            var modelNode = hub.typeSystem.languageServer.findModelNodeByModelFile(this);

            //TODO:直接加载为utf8 bytes,避免字符串转换
            var res = ModelStore.loadServiceCodeAsync(modelNode.model().id()).get();
            Log.debug(String.format("Load model code[%s] from ModelStore.", this.getName()));
            return new ByteArrayInputStream(res.sourceCode.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            Log.warn(String.format("Can't load model's source code: %s", this.getFullPath().toString()));
            throw new CoreException(new ResourceStatus(ResourceStatus.FAILED_READ_LOCAL, ex.getMessage()));
        }
    }

    @Override
    public int getEncoding() throws CoreException {
        return 0;
    }

    @Override
    public IFileState[] getHistory(IProgressMonitor monitor) throws CoreException {
        return new IFileState[0];
    }

    @Override
    public void move(IPath iPath, boolean b, boolean b1, IProgressMonitor monitor) throws CoreException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setCharset(String s) throws CoreException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setCharset(String s, IProgressMonitor monitor) throws CoreException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setContents(InputStream inputStream, boolean b, boolean b1, IProgressMonitor monitor) throws CoreException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setContents(IFileState iFileState, boolean b, boolean b1, IProgressMonitor monitor) throws CoreException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setContents(InputStream inputStream, int i, IProgressMonitor monitor) throws CoreException {
        //TODO:
        //try {
        //    String content = Util.readStreamToString(inputStream);
        //    System.out.println("重新设置文件内空: \n" + content);
        //} catch (Exception e) {
        //    e.printStackTrace();
        //}
    }

    @Override
    public void setContents(IFileState iFileState, int i, IProgressMonitor monitor) throws CoreException {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getType() {
        return IResource.FILE;
    }
}
