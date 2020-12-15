package appbox.design.services.code;

import appbox.design.DesignHub;
import appbox.design.jdt.ModelFile;
import appbox.design.services.CodeGenService;
import appbox.design.tree.ModelNode;
import appbox.logging.Log;
import appbox.model.EntityModel;
import appbox.model.ModelType;
import appbox.runtime.IService;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.core.CompilationUnit;
import org.eclipse.jdt.ls.core.internal.JDTUtils;

public final class TypeSystem {

    public static final String PROJECT_MODELS = "models";

    public final  LanguageServer languageServer;
    protected     IProject       modelsProject; //实体、枚举等通用模型项目
    private final DesignHub      hub;

    public TypeSystem(DesignHub designHub) {
        hub            = designHub;
        languageServer = new LanguageServer(hub.session.sessionId());
        //Do not use languageServer here.
    }

    /**
     * 用于初始化通用项目等
     */
    public void init() {
        try {
            var libAppBoxCorePath = new Path(IService.class.getProtectionDomain()
                    .getCodeSource().getLocation().getPath());
            var libs = new IClasspathEntry[]{
                    JavaCore.newLibraryEntry(libAppBoxCorePath, null, null)
            };
            modelsProject = languageServer.createProject(PROJECT_MODELS, libs);
            //添加基础虚拟文件,从resources中加载
            var sysFolder = modelsProject.getFolder("sys");
            sysFolder.create(true, true, null);

            sysFolder.getFile("EntityBase.java").create(null, true, null);
            sysFolder.getFile("SqlEntityBase.java").create(null, true, null);
            sysFolder.getFile("SysEntityBase.java").create(null, true, null);
            sysFolder.getFile("DbTransaction.java").create(null, true, null);
            sysFolder.getFile("SqlStore.java").create(null, true, null);
            sysFolder.getFile("RuntimeType.java").create(null, true, null);
            sysFolder.getFile("CtorInterceptor.java").create(null, true, null);

            modelsProject.getFile("DataStore.java").create(null, true, null);
            modelsProject.getFile("SqlQuery.java").create(null, true, null);

            //TODO:创建服务代理项目
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** 用于加载设计树后创建模型相应的虚拟文件 */
    public void createModelDocument(ModelNode node) {
        var appName  = node.appNode.model.name();
        var model    = node.model();
        var fileName = String.format("%s.java", model.name());

        //TODO:其他类型模型
        try {
            if (model.modelType() == ModelType.Service) {
                //不再需要加载源码, 注意已签出先从Staged中加载
                var projectName = languageServer.makeServiceProjectName(appName, model.name());
                var project = languageServer.createProject(projectName,
                        new IClasspathEntry[]{JavaCore.newProjectEntry(modelsProject.getFullPath())});

                var file = project.getFile(fileName);
                file.create(null, true, null);
                //TODO:服务模型创建虚拟代理
            } else if (model.modelType() == ModelType.Entity) {
                //需要包含目录,如sys/entities/Order.java
                var appFolder = modelsProject.getFolder(appName);
                if (!appFolder.exists()) {
                    appFolder.create(true, true, null);
                }
                var typeFolder = appFolder.getFolder("entities");
                if (!typeFolder.exists()) {
                    typeFolder.create(true, true, null);
                }
                var file = typeFolder.getFile(fileName);
                file.create(null, true, null);
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
                var appFolder  = modelsProject.getFolder(appName);
                var typeFolder = appFolder.getFolder("entities");
                var file       = typeFolder.getFile(fileName);
                var cu         = (CompilationUnit) JDTUtils.resolveCompilationUnit(file);
                if (cu.getBuffer() != null) {
                    cu.getBuffer().setContents(CodeGenService.genEntityDummyCode(
                            (EntityModel) model, appName, node.designTree()));
                    cu.makeConsistent(null);
                    //Log.debug(cu.getBuffer().getContents());
                }
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
                var appFolder  = modelsProject.getFolder(appName);
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
        //TODO:
    }

    //region ====find XXX====
    public ModelNode findModelNodeByModelFile(ModelFile file) {
        //TODO:暂简单处理路径
        var fileName = file.getName();
        fileName = fileName.substring(0, fileName.length() - 5); //去掉扩展名

        var project = file.getProject();
        if (project.equals(modelsProject)) {
            var typeFolder = file.getParent();
            var appFolder  = typeFolder.getParent();
            var appNode    = hub.designTree.findApplicationNodeByName(appFolder.getName());
            //TODO:其他类型
            return hub.designTree.findModelNodeByName(appNode.model.id(), ModelType.Entity, fileName);
        } else {
            var projectName   = project.getName();
            var firstSepIndex = projectName.indexOf('_');
            var appName       = projectName.substring(0, firstSepIndex);
            var appNode       = hub.designTree.findApplicationNodeByName(appName);
            return hub.designTree.findModelNodeByName(appNode.model.id(), ModelType.Service, fileName);
        }
    }

    /** 找到服务模型对应的虚拟文件 */
    public ModelFile findFileForServiceModel(String appName, String serviceName) {
        var fileName    = String.format("%s.java", serviceName);
        var projectName = String.format("%s_services_%s", appName, serviceName);
        var project     = languageServer.jdtWorkspace.getRoot().getProject(projectName);
        return (ModelFile) project.findMember(fileName);
    }
    //endregion

}
