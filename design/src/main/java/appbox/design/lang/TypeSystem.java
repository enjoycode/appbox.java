package appbox.design.lang;

import appbox.design.DesignHub;
import appbox.design.lang.dart.DartLanguageServer;
import appbox.design.lang.java.JdtLanguageServer;
import appbox.design.tree.ModelNode;
import appbox.model.ModelType;

public final class TypeSystem {

    private final DesignHub          hub;
    public final  JdtLanguageServer  javaLanguageServer;
    public        DartLanguageServer dartLanguageServer;

    public TypeSystem(DesignHub designHub) {
        hub                = designHub;
        javaLanguageServer = new JdtLanguageServer(hub);
        //Do not use languageServer here! has not initialized.
    }

    public void init() {
        javaLanguageServer.init();
    }

    //region ====Model Document====
    public void createModelDocument(ModelNode node, boolean forLoadTree) {
        final var modelType = node.model().modelType();
        //特殊处理加载树时的权限虚拟文件创建
        if (modelType == ModelType.Permission && forLoadTree) {
            javaLanguageServer.filesManager.tryCreatePermissionsFile(node.appNode.model.name());
            return;
        }

        if (node.model().modelType() != ModelType.View)
            javaLanguageServer.filesManager.createModelDocument(node);
    }

    public void updateModelDocument(ModelNode node) {
        javaLanguageServer.filesManager.updateModelDocument(node);
    }

    public void removeModelDocument(ModelNode node) {
        javaLanguageServer.filesManager.removeModelDocument(node);
    }
    //endregion

}
