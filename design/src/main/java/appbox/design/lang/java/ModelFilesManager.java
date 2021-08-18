package appbox.design.lang.java;

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

    private void createModelFile(String appName, String type, String fileName) throws CoreException {
        final var appFolder = languageServer.modelsProject.getFolder(appName);
        if (!appFolder.exists()) {
            appFolder.create(true, true, null);
        }

        if (type == null) { //only for Permissions.java
            final var file = appFolder.getFile(fileName);
            file.create(null, true, null);
        } else {
            final var typeFolder = appFolder.getFolder(type);
            if (!typeFolder.exists()) {
                typeFolder.create(true, true, null);
            }
            final var file = typeFolder.getFile(fileName);
            file.create(null, true, null);
        }
    }

    //endregion

    //region ====Model Files====

    /** 用于加载设计树后创建模型相应的虚拟文件 */
    public void createModelDocument(ModelNode node) {
        var appName  = node.appNode.model.name();
        var model    = node.model();
        var fileName = String.format("%s.java", model.name());

        //TODO:其他类型模型
        try {
            if (model.modelType() == ModelType.Service) {
                //创建服务模型的虚拟工程及代码
                final var projectName = languageServer.makeServiceProjectName(node);
                final var libs        = languageServer.makeServiceProjectDeps(node, false);
                final var project = languageServer.createProject(
                        ModelProject.ModelProjectType.DesigntimeService, projectName, libs);

                final var file = project.getFile(fileName);
                file.create(null, true, null);
                //创建服务模型的虚拟代理(暂放在modelsProject内)
                createModelFile(appName, "services", fileName);
            } else if (model.modelType() == ModelType.Entity) {
                //需要包含目录,如sys/entities/Order.java
                createModelFile(appName, "entities", fileName);
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
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
        var appName  = node.appNode.model.name();
        var model    = node.model();
        var fileName = String.format("%s.java", model.name());

        try {
            if (model.modelType() == ModelType.Entity) {
                var appFolder  = languageServer.modelsProject.getFolder(appName);
                var typeFolder = appFolder.getFolder("entities");
                var file       = typeFolder.getFile(fileName);
                var cu         = JDTUtils.resolveCompilationUnit(file);
                cu.delete(true, null);
                //不需要file.delete(),上一步会调用
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

    /** 用于加载设计树后创建所有权限的虚拟文件，一个应用对应一个权限文件 */
    public void createPermissionsDocuments() {
        var appRootNode = languageServer.hub.designTree.appRootNode();
        for (int i = 0; i < appRootNode.nodes.size(); i++) {
            var appNode        = (ApplicationNode) appRootNode.nodes.get(i);
            var permissionRoot = appNode.findModelRootNode(ModelType.Permission);
            if (!permissionRoot.hasAnyModel())
                continue;

            try {
                createModelFile(appNode.model.name(), null, "Permissions.java");
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    /** 仅用于添加或删除权限模型后更新指定应用的Permissions.java */
    public void updatePermissionsDocument(String appName) {
        var file = languageServer.modelsProject.getFolder(appName).getFile("Permissions.java");
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

}
