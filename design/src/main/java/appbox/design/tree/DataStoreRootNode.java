package appbox.design.tree;

public final class DataStoreRootNode extends DesignNode implements ITopNode {

    private final DesignTree designTree;

    public DataStoreRootNode(DesignTree tree) {
        designTree = tree;
        text       = "DataStore";
    }

    @Override
    public DesignTree getDesignTree() {
        return designTree;
    }

    @Override
    public DesignNodeType nodeType() {
        return DesignNodeType.DataStoreRootNode;
    }
}
