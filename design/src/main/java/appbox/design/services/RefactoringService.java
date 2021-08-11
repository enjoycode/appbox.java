package appbox.design.services;

import appbox.design.DesignHub;
import appbox.design.common.Reference;
import appbox.design.lang.java.jdt.ProgressMonitor;
import appbox.design.lang.java.lsp.ReferencesHandler;
import appbox.model.ModelReferenceType;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.lsp4j.Location;

import java.util.ArrayList;
import java.util.List;

/** 重构服务，目前主要用于查找引用及重命名 */
public final class RefactoringService {

    public static List<Reference> findUsages(DesignHub hub, ModelReferenceType referenceType,
                                             String appName, String modelName, String memberName) {
        throw new RuntimeException();
    }

    private static void addCodeReferences(DesignHub hub, List<Reference> list,
                                          String appName, String modelName, String memberName) {

    }

    private static void addJavaCodeReferences(DesignHub hub, List<Reference> list, IJavaElement symbol) {
        final List<Location> locations = new ArrayList<>();
        try {
            ReferencesHandler.search(symbol, locations, new ProgressMonitor());
            for (var loc : locations) {
                //hub.typeSystem.javaLanguageServer.findModelNodeByModelFile(loc.)
            }
        } catch (CoreException e) {
            e.printStackTrace();
        }
    }

}
