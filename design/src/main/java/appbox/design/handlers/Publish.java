package appbox.design.handlers;

import appbox.design.DesignHub;
import appbox.design.common.PublishPackage;
import appbox.design.services.PublishService;
import appbox.design.services.StagedItems;
import appbox.logging.Log;
import appbox.model.ModelBase;
import appbox.model.ModelFolder;
import appbox.model.ModelType;
import appbox.runtime.InvokeArgs;

import java.util.concurrent.CompletableFuture;

/** IDE发布变更的模型包 */
public final class Publish implements IDesignHandler {

    @Override
    public CompletableFuture<Object> handle(DesignHub hub, InvokeArgs args) {
        var commitMessage = args.getString();

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
            } else if (change instanceof StagedItems.StagedViewRuntimeCode) {
                var viewAsm       = (StagedItems.StagedViewRuntimeCode) change;
                var viewModelNode = hub.designTree.findModelNode(ModelType.View, viewAsm.ModelId);
                var asmName = String.format("%s.%s",
                        viewModelNode.appNode.model.name(), viewModelNode.model().name());
                pkg.viewAssemblies.put(asmName, viewAsm.CodeData);
            } else if (change instanceof ModelFolder) {
                pkg.folders.add((ModelFolder) change);
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
