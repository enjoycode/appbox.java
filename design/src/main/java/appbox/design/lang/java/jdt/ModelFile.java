package appbox.design.lang.java.jdt;

import appbox.design.MockDeveloperSession;
import appbox.design.lang.java.ModelFilesManager;
import appbox.design.services.CodeGenService;
import appbox.design.services.StagedService;
import appbox.logging.Log;
import appbox.model.EntityModel;
import appbox.model.ModelType;
import appbox.store.ModelStore;
import org.eclipse.core.internal.resources.ResourceStatus;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFileState;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.content.IContentDescription;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

//注意：ModelFile不会指向同一实例

public final class ModelFile extends ModelResource implements IFile {

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
        if (local) { //目前仅适用于转译的服务代码及编译生成的class文件
            var file = this.getLocation().toFile();
            try {
                if (!file.exists()) {
                    var parent = file.getParentFile();
                    if (!parent.exists()) {
                        parent.mkdirs();
                    }
                    Files.createFile(file.toPath());
                }
                Files.copy(content, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
                Log.debug("Create file: " + file.getAbsolutePath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

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
        //注意:不要使用RuntimeContext.current()获取上下文,因可能在JobManager的线程内执行
        //先判断是否转译的运行时服务代码
        if (isInRuntimeServiceProject()) {
            var file = this.getLocation().toFile();
            try {
                return new FileInputStream(file);
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

        final var workspace      = (ModelWorkspace) getWorkspace();
        final var languageServer = workspace.languageServer;
        final var session        = languageServer.hub.session;
        final var hub            = languageServer.hub;
        //通过代理加载(仅用于单元测试)
        if (session instanceof MockDeveloperSession && !isInModelsProject()) {
            final var stream = ((MockDeveloperSession) session).loadFileDelegate.apply(this.path);
            if (stream != null)
                return stream;
        }

        //虚拟基础代码从资源文件加载
        if (ModelFilesManager.isDummyFileInResources(this)) {
            Log.debug("Load dummy code: " + getName());
            return ModelFile.class.getResourceAsStream("/dummy/" + getName());
        }

        //DataStore及Permissions特殊生成
        if (ModelFilesManager.isDataStoreFile(this)) {
            var storesDummyCode = CodeGenService.genStoresDummyCode(hub.designTree);
            return new ByteArrayInputStream(storesDummyCode.getBytes(StandardCharsets.UTF_8));
        }
        if (ModelFilesManager.isPermissionsFile(this)) {
            var permissionsDummyCode =
                    CodeGenService.genPermissionsDummyCode(hub.designTree, this.getParent().getName());
            return new ByteArrayInputStream(permissionsDummyCode.getBytes(StandardCharsets.UTF_8));
        }

        //注意:判断当前节点是否签出，是则首先尝试从Staged中加载，再从ModelStore加载代码
        try {
            //根据类型查找模型节点
            var modelNode = languageServer.findModelNodeByModelFile(this);
            //TODO:其他类型处理
            if (modelNode.model().modelType() == ModelType.Entity) {
                //通过CodeGenService生成虚拟代码
                var entityDummyCode = CodeGenService.genEntityDummyCode(
                        (EntityModel) modelNode.model(), modelNode.appNode.model.name(), hub.designTree);
                Log.debug("Generate Entity dummy code:" + this.getName());
                return new ByteArrayInputStream(entityDummyCode.getBytes(StandardCharsets.UTF_8));
            } else if (modelNode.model().modelType() == ModelType.Service) {
                if (isInModelsProject()) { //服务代理
                    var proxyCode = CodeGenService.genServiceProxyCode(hub, modelNode);
                    Log.debug("Generate Service proxy code:" + this.getName());
                    return new ByteArrayInputStream(proxyCode.getBytes(StandardCharsets.UTF_8));
                } else { //服务实现
                    //TODO:**直接加载为utf8 bytes,避免字符串转换
                    if (modelNode.isCheckoutByMe()) {
                        var stagedCode = StagedService.loadServiceCode(modelNode.model().id()).get();
                        if (stagedCode != null) {
                            Log.debug(String.format("Load model code[%s] from Staged.", this.getName()));
                            return new ByteArrayInputStream(stagedCode.getBytes(StandardCharsets.UTF_8));
                        }
                    }

                    var res = ModelStore.loadServiceCodeAsync(modelNode.model().id()).get();
                    Log.debug(String.format("Load model code[%s] from ModelStore.", this.getName()));
                    return new ByteArrayInputStream(res.sourceCode.getBytes(StandardCharsets.UTF_8));
                }
            } else {
                throw new RuntimeException("未实现加载ModelFile: " + getFullPath().toString());
            }
        } catch (Exception ex) {
            Log.warn(String.format("Can't load model's source code: %s", this.getFullPath().toString()));
            ex.printStackTrace();
            throw new CoreException(new ResourceStatus(ResourceStatus.FAILED_READ_LOCAL, ex.getMessage()));
        }
    }

    @Deprecated
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

    @Deprecated
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

    /** 是否隶属于通用模型项目 */
    private boolean isInModelsProject() {
        return getProject() == ((ModelWorkspace) getWorkspace()).languageServer.getModelsProject();
    }

    /** 是否隶属于运行时服务模型项目 */
    private boolean isInRuntimeServiceProject() {
        //TODO:暂简单实现
        return this.path.segment(0).startsWith("runtime_");
    }
}
