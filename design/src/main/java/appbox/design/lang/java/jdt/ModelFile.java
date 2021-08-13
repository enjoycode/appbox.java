package appbox.design.lang.java.jdt;

import appbox.design.MockDeveloperSession;
import appbox.design.services.CodeGenService;
import appbox.design.services.StagedService;
import appbox.design.utils.PathUtil;
import appbox.logging.Log;
import appbox.model.EntityModel;
import appbox.model.ModelType;
import appbox.store.ModelStore;
import org.eclipse.core.internal.resources.File;
import org.eclipse.core.internal.resources.ResourceStatus;
import org.eclipse.core.internal.resources.Workspace;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.jobs.ISchedulingRule;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

//注意：ModelFile不会指向同一实例
//TODO:根据模型标识+版本缓存生成的通用模型的虚拟代码

public final class ModelFile extends File {

    private static final int TEST_FILE               = 0;
    private static final int RESOURCE_FILE           = 1;
    private static final int DATASTORE_FILE          = 2;
    private static final int PERMISSIONS_FILE        = 3;
    private static final int GENERATE_MODEL_FILE     = 4;
    private static final int DESIGNTIME_SERVICE_FILE = 5;
    private static final int RUNTIME_SERVICE_FILE    = 6;

    ModelFile(IPath path, Workspace workspace) {
        super(path, workspace);
    }

    @Override
    public IPath getLocation() {
        return PathUtil.WORKSPACE_PATH.append(getFullPath());
    }

    @Override
    public void create(InputStream content, int updateFlags, IProgressMonitor monitor) throws CoreException {
        final var       workspace = (Workspace) getWorkspace();
        ISchedulingRule rule      = workspace.getRuleFactory().createRule(this);

        try {
            workspace.prepareOperation(rule, monitor);
            workspace.beginOperation(true);
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
        } catch (OperationCanceledException ex) {
            workspace.getWorkManager().operationCanceled();
            throw ex;
        } finally {
            workspace.endOperation(rule, true);
        }
    }

    @Override
    public InputStream getContents(boolean force) throws CoreException {
        //注意:不要使用RuntimeContext.current()获取上下文,因可能在JobManager的线程内执行

        //根据文件类型从不同的来源加载内容
        final var fileType = getFileType();
        //先判断是否转译的运行时服务代码
        if (fileType == RUNTIME_SERVICE_FILE) {
            final var file = this.getLocation().toFile();
            try {
                return new FileInputStream(file);
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

        final var project        = (ModelProject) getProject();
        final var hub            = project.getDesignHub();
        final var languageServer = hub.typeSystem.javaLanguageServer;

        //通过代理加载(仅用于单元测试)
        if (fileType == TEST_FILE ||
                (fileType == DESIGNTIME_SERVICE_FILE && hub.session instanceof MockDeveloperSession)) {
            final var mockSession = (MockDeveloperSession) hub.session;
            if (mockSession.loadFileDelegate != null) {
                final var stream = mockSession.loadFileDelegate.apply(getFullPath());
                if (stream != null)
                    return stream;
            } else {
                throw new RuntimeException("MockSession has not set load file delegate");
            }
        }

        //虚拟基础代码从资源文件加载
        if (fileType == RESOURCE_FILE) {
            Log.debug("Load dummy code: " + getName());
            return ModelFile.class.getResourceAsStream("/dummy/" + getName());
        }

        //DataStore及Permissions特殊生成
        if (fileType == DATASTORE_FILE) {
            var storesDummyCode = CodeGenService.genStoresDummyCode(hub.designTree);
            return new ByteArrayInputStream(storesDummyCode.getBytes(StandardCharsets.UTF_8));
        }
        if (fileType == PERMISSIONS_FILE) {
            var permissionsDummyCode =
                    CodeGenService.genPermissionsDummyCode(hub.designTree, this.getParent().getName());
            return new ByteArrayInputStream(permissionsDummyCode.getBytes(StandardCharsets.UTF_8));
        }

        //注意:判断当前节点是否签出，是则首先尝试从Staged中加载，再从ModelStore加载代码
        try {
            //根据类型查找模型节点
            final var modelNode = languageServer.findModelNodeByModelFile(this);
            //TODO:其他类型处理
            if (modelNode.model().modelType() == ModelType.Entity) {
                //通过CodeGenService生成虚拟代码
                var entityDummyCode = CodeGenService.genEntityDummyCode(
                        (EntityModel) modelNode.model(), modelNode.appNode.model.name(), hub.designTree);
                Log.debug("Generate Entity dummy code:" + this.getName());
                return new ByteArrayInputStream(entityDummyCode.getBytes(StandardCharsets.UTF_8));
            } else if (modelNode.model().modelType() == ModelType.Service) {
                if (fileType == GENERATE_MODEL_FILE) { //服务代理
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

    @Override
    public void setContents(InputStream inputStream, int i, IProgressMonitor monitor) throws CoreException {
        //TODO:
        Log.warn("暂未实现");
        //try {
        //    String content = Util.readStreamToString(inputStream);
        //    System.out.println("重新设置文件内空: \n" + content);
        //} catch (Exception e) {
        //    e.printStackTrace();
        //}
    }

    /** 获取文件类型,如DummyResourceFile */
    private int getFileType() {
        //因为指向实例可能不同,所以只能动态获取
        final var modelProject = (ModelProject) getProject();
        final var projectType  = modelProject.getProjectType();
        switch (projectType) {
            case Models:
                final var parent = getParent();
                if (parent == modelProject) {
                    return getName().equals("DataStore.java") ? DATASTORE_FILE : RESOURCE_FILE;
                } else if (parent.getName().equals("sys") && parent.getParent() == modelProject) {
                    return getName().equals("Permissions.java") ? PERMISSIONS_FILE : RESOURCE_FILE;
                }
                return GENERATE_MODEL_FILE;
            case RuntimeService:
                return RUNTIME_SERVICE_FILE;
            case DesigntimeService:
                return DESIGNTIME_SERVICE_FILE;
            default:
                return TEST_FILE;
        }
    }

}
