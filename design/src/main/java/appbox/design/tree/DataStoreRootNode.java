package appbox.design.tree;

import appbox.design.DesignHub;
import appbox.model.DataStoreModel;

public final class DataStoreRootNode extends DesignNode implements ITopNode {

    private final DesignTree designTree;

    public DataStoreRootNode(DesignTree tree) {
        designTree = tree;
    }

    @Override
    public DesignTree designTree() {
        return designTree;
    }

    @Override
    public DesignNodeType nodeType() {
        return DesignNodeType.DataStoreRootNode;
    }

    @Override
    public String text() {return "DataStore";}

    public DataStoreNode addModel(DataStoreModel model, DesignHub hub, boolean isNew)
    {
        //注意model可能被签出的本地替换掉，所以相关操作必须指向node.Model
        var node = new DataStoreNode(model, hub);
        designTree.bindCheckoutInfo(node, isNew);
        nodes.add(node);
        return node;
    }

}
