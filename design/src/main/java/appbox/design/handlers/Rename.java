package appbox.design.handlers;

import appbox.data.JsonResult;
import appbox.design.DesignHub;
import appbox.design.services.RefactoringService;
import appbox.model.ModelReferenceType;
import appbox.runtime.InvokeArgs;

import java.util.concurrent.CompletableFuture;

/** 重命名模型或其成员 */
public final class Rename implements IDesignHandler {

    @Override
    public CompletableFuture<Object> handle(DesignHub hub, InvokeArgs args) {
        final var refType = ModelReferenceType.fromValue((byte) args.getInt());
        final var modelId = Long.parseUnsignedLong(args.getString());
        final var oldName = args.getString();
        final var newName = args.getString();

        final var list = RefactoringService.renameAsync(hub, refType, modelId, oldName, newName);
        return CompletableFuture.completedFuture(new JsonResult(list));
    }

}
