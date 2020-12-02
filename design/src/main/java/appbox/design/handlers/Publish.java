package appbox.design.handlers;

import appbox.design.DesignHub;
import appbox.design.common.PublishPackage;
import appbox.design.services.PublishService;
import appbox.design.services.StagedItems;
import appbox.logging.Log;
import appbox.model.ModelBase;
import appbox.runtime.InvokeArg;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/** IDE发布变更的模型包 */
public final class Publish implements IRequestHandler {

    @Override
    public CompletableFuture<Object> handle(DesignHub hub, List<InvokeArg> args) {
        var commitMessage = args.get(0).getString();

        if (hub.pendingChanges == null || hub.pendingChanges.length == 0)
            return CompletableFuture.completedFuture(null);

        //将PendingChanges转为PublishPackage
        var pkg = new PublishPackage();
        for (var change : hub.pendingChanges) {
            if (change instanceof ModelBase) {
                pkg.models.add((ModelBase) change);
            } else if (change instanceof StagedItems.StagedSourceCode) {
                var code = (StagedItems.StagedSourceCode) change;
                pkg.sourceCodes.put(code.ModelId, code.CodeData);
            } else {
                Log.warn("Unknow pending change: " + change.getClass().getSimpleName());
            }
        }

        try {
            PublishService.validateModels(hub, pkg);
            PublishService.compileModels(hub, pkg);
        } catch (Exception ex) {
            return CompletableFuture.failedFuture(ex);
        }

        return PublishService.publishAsync(hub, pkg, commitMessage).thenApply(r -> {
            //最后清空临时用的pendingChanges
            hub.pendingChanges = null;
            return null;
        });
    }

}
