package appbox.design.handlers.entity;

import appbox.data.PersistentState;
import appbox.design.DesignHub;
import appbox.design.handlers.IDesignHandler;
import appbox.model.EntityModel;
import appbox.model.ModelType;
import appbox.model.entity.DataFieldModel;
import appbox.model.entity.EntityMemberModel;
import appbox.runtime.InvokeArgs;

import java.util.concurrent.CompletableFuture;

public final class DeleteEntityMember implements IDesignHandler {

    @Override
    public CompletableFuture<Object> handle(DesignHub hub, InvokeArgs args) {
        var modelId    = args.getString();
        var memberName = args.getString();

        var node = hub.designTree.findModelNode(ModelType.Entity, Long.parseUnsignedLong(modelId));
        if (node == null)
            throw new RuntimeException("Can't find entity model node");
        var model = (EntityModel) node.model();
        if (!node.isCheckoutByMe())
            throw new RuntimeException("Node has not checkout");

        var mm = model.getMember(memberName);
        //DataField判断是否外键及被索引使用
        if (mm.type() == EntityMemberModel.EntityMemberType.DataField) {
            var dfm = (DataFieldModel) mm;
            if (dfm.isForeignKey())
                throw new RuntimeException("Can't delete a foreign key member");
            if (model.storeOptions() != null && model.storeOptions().hasIndexes()) {
                for (var index : model.storeOptions().getIndexes()) {
                    //排除已标为删除的
                    if (index.persistentState() == PersistentState.Deleted)
                        continue;
                    if (index.hasMember(mm.memberId()))
                        throw new RuntimeException("Member are used in Index: " + index.name());
                }
            }
        }

        //TODO: 在虚拟工程内查找成员引用

        //移除成员
        model.removeMember(memberName);

        //保存至Staged
        return node.saveAsync(null).thenApply(r -> {
            //更新虚拟文件
            hub.typeSystem.updateModelDocument(node);
            return null;
        });
    }

}
