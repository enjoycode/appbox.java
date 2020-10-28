package appbox.design.services.code;

import appbox.design.DesignHub;
import appbox.design.tree.ModelNode;
import appbox.model.ModelType;

import java.util.HashMap;

public final class TypeSystem {

    public final Workspace workspace;

    public TypeSystem(DesignHub designHub) {
        workspace = new Workspace(designHub);
    }

    /**
     * 用于加载设计树后创建模型相应的虚拟文件
     */
    public void createModelDocument(ModelNode node) {
        //var appName = node.appNode.model.name();
        var model = node.model();

        //TODO:其他类型模型
        if (model.modelType() == ModelType.Service) {
            //var docName = String.format("%s.Services.%s.java", appName, model.name());
            //不再需要加载源码, 注意已签出先从Staged中加载
            //var vf = new ModelVirtualFile(workspace.virtualFileSystem.root.designHub, model.id());
            //workspace.virtualFileSystem.modelFiles.add(vf);

            //TODO:服务模型创建虚拟代理
        }
    }

}
