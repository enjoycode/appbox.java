package appbox.design.lang;

import appbox.design.DesignHub;
import appbox.design.lang.dart.DartLanguageServer;
import appbox.design.lang.java.JdtLanguageServer;
import appbox.design.lang.java.jdt.ModelContainer;
import appbox.design.lang.java.jdt.ModelFile;
import appbox.design.services.CodeGenService;
import appbox.design.tree.ApplicationNode;
import appbox.design.tree.ModelNode;
import appbox.design.utils.CodeHelper;
import appbox.design.utils.PathUtil;
import appbox.logging.Log;
import appbox.model.EntityModel;
import appbox.model.ModelType;
import appbox.model.ServiceModel;
import appbox.runtime.IService;
import appbox.store.SqlStore;
import appbox.store.utils.AssemblyUtil;
import com.ea.async.Async;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.CompilationUnit;
import org.eclipse.jdt.ls.core.internal.JDTUtils;

import java.util.concurrent.CompletableFuture;

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
