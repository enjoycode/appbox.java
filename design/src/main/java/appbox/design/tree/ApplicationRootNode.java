package appbox.design.tree;

public final class ApplicationRootNode extends DesignNode implements ITopNode {

    private final DesignTree designTree;

    public ApplicationRootNode(DesignTree tree) {
        designTree = tree;
    }

    @Override
    public DesignTree getDesignTree() {
        return designTree;
    }

    @Override
    public DesignNodeType getNodeType() {
        return DesignNodeType.ApplicationRoot;
    }
}
