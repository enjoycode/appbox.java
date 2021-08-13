package appbox.design.lang;

import appbox.design.DesignHub;
import appbox.design.lang.dart.DartLanguageServer;
import appbox.design.lang.java.JdtLanguageServer;
import appbox.design.tree.ModelNode;

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
    public void createModelDocument(ModelNode node) {
        javaLanguageServer.filesManager.createModelDocument(node);
    }

    public void updateModelDocument(ModelNode node) {
        javaLanguageServer.filesManager.updateModelDocument(node);
    }

    public void removeModelDocument(ModelNode node) {
        javaLanguageServer.filesManager.removeModelDocument(node);
    }

    public void createPermissionsDocuments() {
        javaLanguageServer.filesManager.createPermissionsDocuments();
    }

    public void updatePermissionsDocument(String appName) {
        javaLanguageServer.filesManager.updatePermissionsDocument(appName);
    }

    public void updateStoresDocument() {
        javaLanguageServer.filesManager.updateStoresDocument();
    }
    //endregion

}
