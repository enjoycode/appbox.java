package appbox.design.handlers.entity;

import appbox.data.JsonResult;
import appbox.design.DesignHub;
import appbox.design.handlers.IRequestHandler;
import appbox.model.EntityModel;
import appbox.runtime.InvokeArg;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class GetEntityModel implements IRequestHandler {
    @Override
    public CompletableFuture<Object> handle(DesignHub hub, List<InvokeArg> args) {
        var modelId=Long.parseUnsignedLong(args.get(0).getString());
        var modelNode = hub.designTree.findModelNode(modelId);
        if (modelNode == null){
            var error = String.format("Cannot find EntityModel: %s", modelId);
            return CompletableFuture.failedFuture(new Exception(error));
        }
        var model = (EntityModel)modelNode.model();
        if(model.sysStoreOptions()!=null){
            //TODO 加载变更添加的索引的构建状态
            return CompletableFuture.supplyAsync(() -> {
                return new JsonResult(modelNode.model());
            });
        } else{
            return CompletableFuture.failedFuture(new Exception("StoreOptions is unknown"));
        }


    }
}
