package appbox.design.handlers.enumm;

import appbox.design.DesignHub;
import appbox.design.handlers.IDesignHandler;
import appbox.model.EnumModel;
import appbox.model.ModelType;
import appbox.runtime.InvokeArgs;

import java.util.concurrent.CompletableFuture;

public class GetEnumItems implements IDesignHandler {
    @Override
    public CompletableFuture<Object> handle(DesignHub hub, InvokeArgs args) {
        var modelID = Long.parseUnsignedLong(args.getString());
        var modelNode = hub.designTree.findModelNode(ModelType.Enum, modelID);
        if (modelNode == null) {
            throw new RuntimeException("Can't find EnumModel.");
        }
        var enumModel = (EnumModel)modelNode.model();
        return CompletableFuture.completedFuture(enumModel.items);
    }
}
