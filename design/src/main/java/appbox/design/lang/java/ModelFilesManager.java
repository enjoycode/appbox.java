package appbox.design.lang.java;

import appbox.data.PersistentState;
import appbox.design.lang.java.jdt.ModelProject;
import appbox.design.services.CodeGenService;
import appbox.design.tree.ApplicationNode;
import appbox.design.tree.ModelNode;
import appbox.logging.Log;
import appbox.model.EntityModel;
import appbox.model.ModelType;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.CompilationUnit;
import org.eclipse.jdt.internal.core.Openable;
import org.eclipse.jdt.internal.core.OpenableElementInfo;
import org.eclipse.jdt.ls.core.internal.JDTUtils;

/** 管理所有虚拟文件 */
public final class ModelFilesManager {

    //region ====Consts====
    /** /models/sys/下的虚拟文件列表 */
    public static final String[] SYS_DUMMY_FILES = new String[]{
            "Async.java",
            "EntityBase.java",
            "SqlEntityBase.java",
            "SysEntityBase.java",
            "DbTransaction.java",
            "SqlStore.java",
            "RuntimeType.java",
            "CtorInterceptor.java",
            "MethodInterceptor.java",
            "ISqlQueryJoin.java",
            "ISqlIncluder.java",
            "ISqlIncludable.java",
            "KVTransaction.java"
    };

    /** /models/下的虚拟文件列表 */
    public static final String[] ROOT_DUMMY_FILES = new String[]{
            "DataStore.java",
            "SqlQuery.java",
            "SqlQueryJoin.java",
            "SqlSubQuery.java",
            "SqlUpdateCommand.java",
            "SqlDeleteCommand.java",
            "DbFunc.java",
            "Authorize.java",
            "TableScan.java"
    };
    //endregion

    private final JdtLanguageServer languageServer;

    public ModelFilesManager(JdtLanguageServer languageServer) {
        this.languageServer = languageServer;
    }

    //region ====Dummy Files====

    /** 在指定目录下创建虚拟文件 */
    public static void createDummyFiles(IContainer container, String[] files) throws CoreException {
        for (var fileName : files) {
            final var file = container.getFile(new Path(fileName));
            file.create(null, true, null);
        }
    }

    private IFile createModelFile(String appName, String type, String fileName) throws CoreException {
        final var appFolder = languageServer.modelsProject.getFolder(appName);
        if (!appFolder.exists()) {
            appFolder.create(true, true, null);
        }

        if (type == null) { //only for Permissions.java
            final var file = appFolder.getFile(fileName);
            file.create(null, true, null);
            return file;
        } else {
            final var typeFolder = appFolder.getFolder(type);
            if (!typeFolder.exists()) {
                typeFolder.create(true, true, null);
            }
            final var file = typeFolder.getFile(fileName);
            file.create(null, true, null);
            return file;
        }
    }

    //endregion

    //region ====Model Files====

    /** 用于加载设计树后或新建模型时创建相应的虚拟文件 */
    public void createModelDocument(ModelNode node) {
        final var appName  = node.appNode.model.name();
        final var model    = node.model();
        final var fileName = String.format("%s.java", model.name());
        //如果是新建的或重命名的(临时用),则需要添加至JavaModel缓存
        final var addToCache = model.persistentState() == PersistentState.Detached || model.isNameChanged();

        //TODO:其他类型模型
        try {
            switch (model.modelType()) {
                case Service:
                    createServiceModelDocument(node, appName, fileName, addToCache);
                    break;
                case Entity:
                    createEntityModelDocument(appName, fileName, addToCache);
                    break;
                case Permission:
                    updatePermissionsDocument(appName);
                    break;
                default:
                    Log.warn("暂未实现: " + model.modelType().toString());
                    break;
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private void createServiceModelDocument(ModelNode node, String appName,
                                            String fileName, boolean addToCache) throws Exception {
        //创建服务模型的虚拟工程及代码
        final var projectName = languageServer.makeServiceProjectName(node);
        final var libs        = languageServer.makeServiceProjectDeps(node, false);
        final var project = languageServer.createProject(
                ModelProject.ModelProjectType.DesigntimeService, projectName, libs);

        var file = project.getFile(fileName);
        file.create(null, true, null);
        //创建服务模型的虚拟代理(暂放在modelsProject内)
        file = createModelFile(appName, "services", fileName);
        if (addToCache) {
            addToParentInfo((Openable) JDTUtils.resolveCompilationUnit(file));
        }
    }

    private void createEntityModelDocument(String appName, String fileName, boolean addToCache) throws Exception {
        //需要包含目录,如sys/entities/Order.java
        var file = createModelFile(appName, "entities", fileName);
        if (addToCache) {
            addToParentInfo((Openable) JDTUtils.resolveCompilationUnit(file));
        }
    }

    /** 注意：服务模型也会更新，如不需要由调用者忽略 */
    public void updateModelDocument(ModelNode node) {
        var appName  = node.appNode.model.name();
        var model    = node.model();
        var fileName = String.format("%s.java", model.name());

        try {
            if (model.modelType() == ModelType.Entity) {
                var appFolder  = languageServer.modelsProject.getFolder(appName);
                var typeFolder = appFolder.getFolder("entities");
                var file       = typeFolder.getFile(fileName);
                var newContent = CodeGenService.genEntityDummyCode((EntityModel) model
                        , appName, languageServer.hub.designTree);
                updateFileContent(file, newContent);
            } else {
                Log.warn("updateModelDocument暂未实现: " + model.modelType().name());
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public void removeModelDocument(ModelNode node) {
        final var appName  = node.appNode.model.name();
        final var model    = node.model();
        final var fileName = String.format("%s.java", model.name());

        try {
            if (model.modelType() == ModelType.Entity) {
                final var appFolder  = languageServer.modelsProject.getFolder(appName);
                final var typeFolder = appFolder.getFolder("entities");
                final var file       = typeFolder.getFile(fileName);
                final var cu         = JDTUtils.resolveCompilationUnit(file);
                cu.delete(true, null);
                //不需要file.delete(),上一步会调用并且call JavaProject.resetCaches()
                //但需要从JavaModel cache中移除
                removeFromParentInfo((Openable) cu);
            } else if (model.modelType() == ModelType.Permission) {
                updatePermissionsDocument(appName);
            } else {
                Log.warn("removeModelDocument 未实现");
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /** 仅用于添加或删除存储模型后更新DataStore.java虚拟代码 */
    public void updateStoresDocument() {
        //TODO:*****
    }

    /** 尝试创建Permissions虚拟文件,已存在则忽略 */
    public void tryCreatePermissionsFile(String appName) {
        final var file = languageServer.modelsProject.getFolder(appName).getFile("Permissions.java");
        try {
            if (!file.exists())
                file.create(null, true, null);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /** 仅用于添加或删除权限模型后更新指定应用的Permissions.java */
    public void updatePermissionsDocument(String appName) {
        final var file = languageServer.modelsProject.getFolder(appName).getFile("Permissions.java");
        try {
            if (file.exists()) {
                var newContent = CodeGenService.genPermissionsDummyCode(languageServer.hub.designTree, appName);
                updateFileContent(file, newContent);
            } else {
                file.create(null, true, null);
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static void updateFileContent(IFile file, String newContent) throws JavaModelException {
        var cu = (CompilationUnit) JDTUtils.resolveCompilationUnit(file);
        if (cu.getBuffer() != null) {
            cu.getBuffer().setContents(newContent);
            cu.makeConsistent(null);
        } else {
            Log.warn("Can't get buffer from file: " + file.getName());
        }
    }

    //endregion

    //region ====Java ModelUpdater====

    /** Adds the given child handle to its parent's cache of children. */
    private static void addToParentInfo(Openable child) {
        Openable parent = (Openable) child.getParent();
        if (parent != null && parent.isOpen()) {
            try {
                OpenableElementInfo info = (OpenableElementInfo) parent.getElementInfo();
                info.addChild(child);
            } catch (JavaModelException e) {
                // do nothing - we already checked if open
            }
        }
    }

    /**
     * Removes the given element from its parents cache of children. If the
     * element does not have a parent, or the parent is not currently open,
     * this has no effect.
     */
    private static void removeFromParentInfo(Openable child) {
        Openable parent = (Openable) child.getParent();
        if (parent != null && parent.isOpen()) {
            try {
                OpenableElementInfo info = (OpenableElementInfo) parent.getElementInfo();
                info.removeChild(child);
            } catch (JavaModelException e) {
                // do nothing - we already checked if open
            }
        }
    }
    //endregion
}
