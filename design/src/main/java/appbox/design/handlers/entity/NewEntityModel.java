package appbox.design.handlers.entity;

import appbox.design.DesignHub;
import appbox.design.handlers.IDesignHandler;
import appbox.design.handlers.ModelCreator;
import appbox.design.tree.DesignNodeType;
import appbox.model.DataStoreModel;
import appbox.model.EntityModel;
import appbox.model.ModelType;
import appbox.runtime.InvokeArgs;

import java.util.concurrent.CompletableFuture;

public class NewEntityModel implements IDesignHandler {

    @Override
    public CompletableFuture<Object> handle(DesignHub hub, InvokeArgs args) {
        var selectedNodeType = DesignNodeType.fromValue((byte) args.getInt());
        var selectedNodeId   = args.getString();
        var name             = args.getString();
        var localizedName    = args.getString();
        var storeName        = args.getString();
        var orderByDesc      = args.getBool();

        return ModelCreator.create(hub, ModelType.Entity, id -> {
            //根据映射的存储创建相应的实体模型
            var entityModel = new EntityModel(id, name);
            if (storeName != null && !storeName.isEmpty()) {
                var storeNode = hub.designTree.findDataStoreNodeByName(storeName);
                if (storeNode == null)
                    throw new RuntimeException("Can't find DataStore: " + storeName);
                if (storeNode.model().kind() == DataStoreModel.DataStoreKind.Future) {
                    entityModel.bindToSysStore(true, false); //TODO: fix options
                } else if (storeNode.model().kind() == DataStoreModel.DataStoreKind.Sql) {
                    entityModel.bindToSqlStore(storeNode.model().id());
                    entityModel.sqlStoreOptions().setDataStoreModel(storeNode.model());
                } else {
                    throw new RuntimeException("未实现");
                }
            }

            //TODO:set localizedName
            return entityModel;
        }, selectedNodeType, selectedNodeId, name, null);
    }

}
