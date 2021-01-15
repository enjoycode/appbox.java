package appbox.design.handlers.service;

import appbox.design.DesignHub;
import appbox.design.handlers.IDesignHandler;
import appbox.design.handlers.ModelCreator;
import appbox.design.tree.DesignNodeType;
import appbox.model.ModelType;
import appbox.model.ServiceModel;
import appbox.runtime.InvokeArgs;

import java.util.concurrent.CompletableFuture;

public final class NewServiceModel implements IDesignHandler {

    @Override
    public CompletableFuture<Object> handle(DesignHub hub, InvokeArgs args) {
        // 获取接收到的参数
        var selectedNodeType = DesignNodeType.fromValue((byte) args.getInt());
        var selectedNodeId   = args.getString();
        var name             = args.getString();
        var initServiceCode  = String.format("public class %s\n{\n}", name);
        var initCodes        = new Object[]{initServiceCode};

        return ModelCreator.create(hub, ModelType.Service, (id) -> new ServiceModel(id, name),
                selectedNodeType, selectedNodeId, name, initCodes);
    }
}
