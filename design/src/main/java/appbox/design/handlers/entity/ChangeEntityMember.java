package appbox.design.handlers.entity;

import appbox.design.DesignHub;
import appbox.design.handlers.IDesignHandler;
import appbox.model.EntityModel;
import appbox.model.ModelType;
import appbox.runtime.InvokeArgs;

import java.util.concurrent.CompletableFuture;

/** 改变实体成员的设置,如备注等 */
public final class ChangeEntityMember implements IDesignHandler {

    @Override
    public CompletableFuture<Object> handle(DesignHub hub, InvokeArgs args) {
        final var modelId       = Long.parseUnsignedLong(args.getString());
        final var memberName    = args.getString();
        final var propertyName  = args.getString();
        final var propertyValue = args.getString();

        final var modelNode = hub.designTree.findModelNode(ModelType.Entity, modelId);
        if (modelNode == null)
            return CompletableFuture.failedFuture(new RuntimeException("Can't find Entity model"));

        final var model  = (EntityModel) modelNode.model();
        final var member = model.tryGetMember(memberName);
        if (member == null)
            return CompletableFuture.failedFuture(new RuntimeException("Can't find " + model.name() + "." + memberName));
        //TODO:如果改变DataField数据类型预先检查兼容性
        switch (propertyName) {
            case "Comment":
                member.setComment(propertyValue);
                break;
            case "AllowNull":
                //TODO:判断是否允许,即是否有空记录存在
                member.setAllowNull(Boolean.parseBoolean(propertyValue));
                break;
            default:
                return CompletableFuture.failedFuture(new RuntimeException("暂未实现"));
        }

        return CompletableFuture.completedFuture(null);
    }

}
