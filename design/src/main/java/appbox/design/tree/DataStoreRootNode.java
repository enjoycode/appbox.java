package appbox.design.tree;

import appbox.data.PersistentState;
import appbox.design.DesignHub;
import appbox.model.DataStoreModel;

public final class DataStoreRootNode extends DesignNode implements ITopNode {

    private final DesignTree designTree;

    public DataStoreRootNode(DesignTree tree) {
        designTree = tree;
        text       = "DataStore";
    }

    @Override
    public DesignTree designTree() {
        return designTree;
    }

    @Override
    public DesignNodeType nodeType() {
        return DesignNodeType.DataStoreRootNode;
    }

    public DataStoreNode addModel(DataStoreModel model, DesignHub hub)
    {
        //注意model可能被签出的本地替换掉，所以相关操作必须指向node.Model
        var node = new DataStoreNode(model, hub);
        designTree.bindCheckoutInfo(node, model.persistentState() == PersistentState.Detached);
        nodes.add(node);
        return node;
    }

}
