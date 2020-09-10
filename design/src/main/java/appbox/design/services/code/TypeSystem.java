package appbox.design.services.code;

import appbox.design.tree.ModelNode;
import appbox.model.ModelType;

import java.util.HashMap;

public final class TypeSystem {

    public final HashMap<String, JavaDocument> opendDocs = new HashMap<>(); //TODO:移至Workspace内
    public final Workspace workspace;

    public TypeSystem() {
        workspace = new Workspace(this);
    }

    public void createModelDocument(ModelNode node) {
        var appName = node.appNode.model.name();
        var model   = node.model();

        if (model.modelType() == ModelType.Service) {
            var docName = String.format("%s.Services.%s.java", appName, model.name());
            //加载源码, TODO:已签出先从Staged中加载

            //TODO:服务模型创建虚拟代理
        }

    }

}
