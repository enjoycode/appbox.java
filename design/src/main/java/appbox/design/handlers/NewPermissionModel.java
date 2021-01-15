package appbox.design.handlers;

import appbox.design.DesignHub;
import appbox.design.tree.DesignNodeType;
import appbox.model.ModelType;
import appbox.model.PermissionModel;
import appbox.runtime.InvokeArgs;

import java.util.concurrent.CompletableFuture;

public final class NewPermissionModel implements IDesignHandler {

    @Override
    public CompletableFuture<Object> handle(DesignHub hub, InvokeArgs args) {
        var selectedNodeType = DesignNodeType.fromValue((byte) args.getInt());
        var selectedNodeId   = args.getString();
        var name             = args.getString();

        return ModelCreator.create(hub, ModelType.Permission, id -> new PermissionModel(id, name),
                selectedNodeType, selectedNodeId, name, null);
    }

}
