package appbox.design.handlers.view;

import appbox.design.DesignHub;
import appbox.design.handlers.IDesignHandler;
import appbox.design.handlers.ModelCreator;
import appbox.design.tree.DesignNodeType;
import appbox.model.ModelType;
import appbox.model.ViewModel;
import appbox.runtime.InvokeArgs;

import java.util.concurrent.CompletableFuture;

public class NewViewModel implements IDesignHandler {

    @Override
    public CompletableFuture<Object> handle(DesignHub hub, InvokeArgs args) {
        var selectedNodeType = DesignNodeType.fromValue((byte) args.getInt());
        var selectedNodeId   = args.getString();
        var name             = args.getString();

        var templateCode = "<div>Hello Future!</div>";
        var scriptCode   = String.format("@Component\nexport default class %s extends Vue {\n\n}\n", name);
        var initCodes    = new Object[]{templateCode, scriptCode, "", ""};

        return ModelCreator.create(hub, ModelType.View, id -> new ViewModel(id, name)
                , selectedNodeType, selectedNodeId, name, initCodes);
    }
}
