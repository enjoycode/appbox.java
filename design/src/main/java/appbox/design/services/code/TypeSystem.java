package appbox.design.services.code;

import appbox.design.DesignHub;
import appbox.design.jdt.ModelFile;
import appbox.design.tree.ModelNode;
import appbox.logging.Log;
import appbox.model.ModelType;
import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;

public final class TypeSystem {

    public final  LanguageServer languageServer;
    private       IProject       modelsProject;
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
            //创建实体、枚举等通用模型项目
            modelsProject = languageServer.createProject("models", null);
            //TODO:创建服务代理项目
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** 用于加载设计树后创建模型相应的虚拟文件 */
    public void createModelDocument(ModelNode node) throws Exception {
        var appName  = node.appNode.model.name();
        var model    = node.model();
        var fileName = String.format("%s.java", model.name());

        //TODO:其他类型模型
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
    }

    /** 注意：服务模型也会更新，如不需要由调用者忽略 */
    public void updateModelDocument(ModelNode node) {
        Log.warn("updateModelDocument暂未实现");
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
