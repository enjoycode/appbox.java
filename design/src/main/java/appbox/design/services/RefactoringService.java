package appbox.design.services;

import appbox.design.DesignHub;
import appbox.design.common.CodeReference;
import appbox.design.common.Reference;
import appbox.design.lang.java.jdt.ProgressMonitor;
import appbox.design.lang.java.lsp.ReferencesHandler;
import appbox.design.tree.DesignNodeType;
import appbox.design.tree.ModelNode;
import appbox.model.EntityModel;
import appbox.model.ModelReferenceType;
import appbox.model.ModelType;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.lsp4j.Location;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

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

    public static String[] renameAsync(DesignHub hub, ModelReferenceType referenceType,
                                       long modelId, String oldName, String newName) {
        //1.先判断当前模型是否已签出
        ModelNode sourceNode = null;
        switch (referenceType) {
            case EntityMemberName:
                sourceNode = hub.designTree.findModelNode(ModelType.Entity, modelId);
                break;
            default:
                throw new RuntimeException("暂未实现");
        }
        if (!sourceNode.isCheckoutByMe()) {
            throw new RuntimeException("ModelNode has not checkout: " + sourceNode.text());
        }

        //2.查找引用项并排序,同时判断是否签出
        final var references =
                findUsages(hub, referenceType, sourceNode.appNode.model.name(), sourceNode.model().name(), oldName);
        references.sort((l, r) -> r.compareTo(l)); //注意倒序
        for (var item : references) {
            if (!item.modelNode.isCheckoutByMe()) {
                throw new RuntimeException("ModelNode has not checkout: " + item.modelNode.text());
            }
        }

        //3.开始重命名操作
        final var needClose = new HashMap<Long, Reference>(); //仅用于未打开的服务代码
        for (final var r : references) {
            //判断引用类型,分别处理
            if (r instanceof CodeReference) {
                boolean hasOpen = doRenameCodeReference(hub, (CodeReference) r, newName);
                if (!hasOpen)
                    needClose.put(r.modelNode.model().id(), r);
            } else {
                throw new RuntimeException("暂未实现");
            }
        }

        //4.保存并关闭相关
        final var affects = references.stream().map(v -> v.modelNode)
                .distinct().collect(Collectors.toList());
        for (var node : affects) {
            node.saveAsync(null).join();
        }
        for (var id : needClose.keySet()) {
            hub.typeSystem.javaLanguageServer.closeDocument(id);
        }

        //5.处理源模型的重命名
        switch (referenceType) {
            case EntityMemberName:
                ((EntityModel) sourceNode.model()).renameMember(oldName, newName);
                sourceNode.saveAsync(null).join();
                hub.typeSystem.updateModelDocument(sourceNode);
                break;
            default:
                throw new RuntimeException("暂未实现");
        }

        //最后返回处理结果,暂简单返回受影响的节点标识集合(暂不包括源),由前端刷新
        return affects.stream()
                .map(v -> Long.toUnsignedString(v.model().id()))
                .toArray(String[]::new);
    }

    /** 开始执行代码引用的重命名 */
    private static boolean doRenameCodeReference(DesignHub hub, CodeReference reference, String newName) {
        final var modelNode = reference.modelNode;
        if (modelNode.nodeType() != DesignNodeType.ServiceModelNode) {
            throw new UnsupportedOperationException();
        }

        //TODO:暂简单实现,应该区分已打开及未打开的分别实现
        boolean hasOpen = true;
        var     doc     = hub.typeSystem.javaLanguageServer.findOpenedDocument(modelNode.model().id());
        if (doc == null) {
            hasOpen = false;
            doc     = hub.typeSystem.javaLanguageServer.openDocument(modelNode);
        }
        final var startOffset = doc.getOffset(reference.range.getStart().getLine(),
                reference.range.getStart().getCharacter());
        final var endOffset = doc.getOffset(reference.range.getEnd().getLine(),
                reference.range.getEnd().getCharacter());
        final var oldLength = endOffset - startOffset;

        hub.typeSystem.javaLanguageServer.changeDocument(doc, startOffset, oldLength, newName);
        //doc.changeText(startOffset, oldLength, newName);

        return hasOpen;
    }
}
