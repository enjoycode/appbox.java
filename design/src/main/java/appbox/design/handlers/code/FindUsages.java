package appbox.design.handlers.code;

import appbox.design.DesignHub;
import appbox.design.handlers.IDesignHandler;
import appbox.design.services.RefactoringService;
import appbox.model.ModelReferenceType;
import appbox.runtime.InvokeArgs;

import java.util.concurrent.CompletableFuture;

/** 查找模型或模型成员的引用项 */
public final class FindUsages implements IDesignHandler {

    @Override
    public CompletableFuture<Object> handle(DesignHub hub, InvokeArgs args) {
        final var refType    = ModelReferenceType.fromValue((byte) args.getInt());
        final var modelId    = Long.parseUnsignedLong(args.getString());
        final var memberName = args.getString(); //nullable

        final var modelNode = hub.designTree.findModelNode(modelId);
        if (modelNode == null)
            return CompletableFuture.failedFuture(new RuntimeException("Can't find model"));

        final var list = RefactoringService.findUsages(hub, refType,
                modelNode.appNode.model.name(), modelNode.model().name(), memberName);
        return CompletableFuture.completedFuture(list);
    }

}
