package appbox.design.services.code;

import appbox.design.DesignHub;
import appbox.design.tree.ModelNode;
import appbox.model.ModelType;

public final class TypeSystem {

    public final LanguageServer languageServer;

    public TypeSystem(DesignHub designHub) {
        languageServer = new LanguageServer(designHub);
    }

    /**
     * 用于加载设计树后创建模型相应的虚拟文件
     */
    public void createModelDocument(ModelNode node) throws Exception {
        var appName = node.appNode.model.name();
        var model = node.model();

        //TODO:其他类型模型
        if (model.modelType() == ModelType.Service) {
            //不再需要加载源码, 注意已签出先从Staged中加载
            var projectName = String.format("%s_services_%s", appName, model.name());
            var project = languageServer.createProject(projectName);
            var fileName = String.format("%s.java", model.name());
            languageServer.createFile(project, fileName);

            //TODO:服务模型创建虚拟代理
        }
    }

}
