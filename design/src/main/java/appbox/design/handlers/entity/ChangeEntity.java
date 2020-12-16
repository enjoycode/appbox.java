package appbox.design.handlers.entity;

import appbox.data.PersistentState;
import appbox.design.DesignHub;
import appbox.design.handlers.IDesignHandler;
import appbox.logging.Log;
import appbox.model.EntityModel;
import appbox.model.ModelType;
import appbox.model.entity.EntityMemberModel;
import appbox.model.entity.EntityRefModel;
import appbox.model.entity.FieldWithOrder;
import appbox.runtime.InvokeArgs;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

public final class ChangeEntity implements IDesignHandler {

    @Override
    public CompletableFuture<Object> handle(DesignHub hub, InvokeArgs args) {
        var modelId    = Long.parseUnsignedLong(args.getString());
        var changeType = args.getString();

        var node = hub.designTree.findModelNode(ModelType.Entity, modelId);
        if (node == null)
            throw new RuntimeException("Can't find entity");
        var model = (EntityModel) node.model();
        if (!node.isCheckoutByMe())
            throw new RuntimeException("Node hasn't checkout");

        //注意某些操作需要更新虚拟代码
        switch (changeType) {
            case "PrimaryKeys":
                changePrimaryKeys(model, args.getString());
                hub.typeSystem.updateModelDocument(node);
                break;
            default:
                throw new RuntimeException("Not supported: " + changeType);
        }

        return CompletableFuture.completedFuture(null);
    }

    private static void changePrimaryKeys(EntityModel entityModel, String json) {
        if (entityModel.sqlStoreOptions() == null)
            throw new RuntimeException("Only for sql store");

        if (entityModel.persistentState() != PersistentState.Detached) {
            //TODO:如果是修改则必须查找服务方法内的引用，签出节点并修改
            //1. new XXXX(pks)改为new XXX(/*fix pk changed*/)
            //2. Entities.XXX.LoadAsync(pks)同上
            Log.warn("修改实体主键时签出相关引用");
        }

        var array = JSON.parseArray(json);
        if (array.size() == 0) {
            entityModel.sqlStoreOptions().setPrimaryKeys(null);
        } else {
            var pks = new ArrayList<FieldWithOrder>(array.size());
            for (int i = 0; i < array.size(); i++) {
                var jobj = ((JSONObject) array.get(i));
                //注意如果选择的是EntityRef，则加入所有外键成员作为主键
                var memberId    = jobj.getShortValue("MemberId");
                var memberModel = entityModel.getMember(memberId);
                if (memberModel.type() == EntityMemberModel.EntityMemberType.DataField) {
                    pks.add(new FieldWithOrder(memberId, jobj.getBooleanValue("OrderByDesc")));
                } else if (memberModel.type() == EntityMemberModel.EntityMemberType.EntityRef) {
                    var refMemberModel = (EntityRefModel) memberModel;
                    for (var fk : refMemberModel.getFKMemberIds()) {
                        pks.add(new FieldWithOrder(fk, jobj.getBooleanValue("OrderByDesc")));
                    }
                } else {
                    throw new RuntimeException("Not supported");
                }
            }
            entityModel.sqlStoreOptions().setPrimaryKeys(pks.toArray(FieldWithOrder[]::new));
        }
    }

}
