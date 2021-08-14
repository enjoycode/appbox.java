package appbox.design.services;

import appbox.design.DesignHub;
import appbox.design.common.CodeReference;
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
        if (referenceType == ModelReferenceType.EntityMemberName) {
            return findEntityMemberReferences(hub, appName, modelName, memberName);
        } else {
            throw new RuntimeException("暂未实现");
        }
    }

    /** 查找实体模型成员的所有引用 */
    private static List<Reference> findEntityMemberReferences(
            DesignHub hub, String appName, String modelName, String memberName) {
        final var list = new ArrayList<Reference>();

        //TODO:查找实体模型本身及所有其他实体模型的相关表达式（组织策略、编辑策略等）的引用
        //TODO:查找视图模型的虚拟代码引用

        //查找服务模型的虚拟代码引用
        final var memberSymbol = hub.typeSystem.javaLanguageServer.symbolFinder
                .getEntityMemberSymbol(appName, modelName, memberName);
        addJavaCodeReferences(hub, list, memberSymbol);

        return list;
    }


    private static void addJavaCodeReferences(DesignHub hub, List<Reference> list, IJavaElement symbol) {
        final List<Location> locations = new ArrayList<>();
        try {
            ReferencesHandler.search(symbol, locations, new ProgressMonitor());
            for (var loc : locations) {
                var modelNode = hub.typeSystem.javaLanguageServer.findModelNodeByUri(loc.getUri());
                var reference = new CodeReference(modelNode, loc.getRange());
                list.add(reference);
            }
        } catch (CoreException e) {
            e.printStackTrace();
        }
    }

}
