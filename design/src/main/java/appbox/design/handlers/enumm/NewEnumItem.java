package appbox.design.handlers.enumm;

import appbox.design.DesignHub;
import appbox.design.handlers.IDesignHandler;
import appbox.design.utils.CodeHelper;
import appbox.model.EnumModel;
import appbox.model.EnumModelItem;
import appbox.model.ModelType;
import appbox.runtime.InvokeArgs;

import java.util.concurrent.CompletableFuture;

public class NewEnumItem implements IDesignHandler {

    @Override
    public CompletableFuture<Object> handle(DesignHub hub, InvokeArgs args) {
        var modelId = Long.parseUnsignedLong(args.getString());
        String itemName = args.getString();
        int value = args.getInt();
        String comment = args.getString();

        var node = hub.designTree.findModelNode(ModelType.Enum, modelId);
        if (node == null) {
            throw new RuntimeException("Can't find enum model node");
        }
        var model = (EnumModel)node.model();
        if (!node.isCheckoutByMe()) {
            throw new RuntimeException("Node has not checkout");
        }
        if (!CodeHelper.isValidIdentifier(itemName)) {
            throw new RuntimeException("Name is invalid");
        }
        if (itemName.equals(model.name())) {
            throw new RuntimeException("Name can not same as Enum name");
        }
        if(model.items.stream().filter(t->t.name.equals(itemName)).count()>0){
            throw new RuntimeException("Name has exists");
        }

        if (model.items.stream().filter(t->t.value==value).count()>0){
            throw new RuntimeException("Value has exists");
        }

        var item = new EnumModelItem(itemName, value);
        if (comment!=null&&!comment.equals("")) {
            item.setComment(comment);
        }
        item.setName(itemName);
        item.setValue(value);
        model.items.add(item);

        // 保存到本地
        return node.saveAsync(null).thenApply(r->{
            hub.typeSystem.updateModelDocument(node);
            return item;
        });

    }
}
