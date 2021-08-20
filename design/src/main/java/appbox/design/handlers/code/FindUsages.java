package appbox.design.handlers.code;

import appbox.data.JsonResult;
import appbox.design.DesignHub;
import appbox.design.common.CodeReference;
import appbox.design.handlers.IDesignHandler;
import appbox.design.services.RefactoringService;
import appbox.design.tree.DesignNodeType;
import appbox.model.ModelReferenceType;
import appbox.runtime.InvokeArgs;

import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/** 查找模型或模型成员的引用项 */
public final class FindUsages implements IDesignHandler {

    @Override
    public CompletableFuture<Object> handle(DesignHub hub, InvokeArgs args) {
        final var refType = ModelReferenceType.fromValue((byte) args.getInt());
        if (refType == ModelReferenceType.CodeEditor) {
            final var fileName = args.getString(); //TODO:考虑修改前端传模型标识
            final var line     = args.getInt() - 1; //前端值-1
            final var column   = args.getInt() - 1;

            var modelNode = hub.designTree.findModelNodeByFileName(fileName);
            if (modelNode == null)
                return CompletableFuture.failedFuture(new RuntimeException("Can't find model: " + fileName));
            if (modelNode.nodeType() == DesignNodeType.ServiceModelNode) {
                final var modelId = modelNode.model().id();
                final var doc     = hub.typeSystem.javaLanguageServer.findOpenedDocument(modelId);
                if (doc == null) {
                    var error = String.format("Can't find opened ServiceModel: %s", fileName);
                    return CompletableFuture.failedFuture(new RuntimeException(error));
                }

                final var locations = hub.typeSystem.javaLanguageServer.references(doc, line, column);
                //暂转换为CodeReference以适配前端
                final var resList = locations.stream().map(loc -> {
                    var node = hub.typeSystem.javaLanguageServer.findModelNodeByUri(loc.getUri());
                    return new CodeReference(modelNode, loc.getRange());
                }).collect(Collectors.toList());
                return CompletableFuture.completedFuture(new JsonResult(resList));
            } else {
                return CompletableFuture.failedFuture(new RuntimeException("暂未实现"));
            }
        } else {
            final var modelId    = Long.parseUnsignedLong(args.getString());
            final var memberName = args.getString(); //nullable

            final var modelNode = hub.designTree.findModelNode(modelId);
            if (modelNode == null)
                return CompletableFuture.failedFuture(new RuntimeException("Can't find model"));

            final var list = RefactoringService.findUsages(hub, refType,
                    modelNode.appNode.model.name(), modelNode.model().name(), memberName);
            return CompletableFuture.completedFuture(new JsonResult(list));
        }
    }

}
