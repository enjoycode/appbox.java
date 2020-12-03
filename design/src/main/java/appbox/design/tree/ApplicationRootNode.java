package appbox.design.tree;

public final class ApplicationRootNode extends DesignNode implements ITopNode {

    private final DesignTree designTree;

    public ApplicationRootNode(DesignTree tree) {
        designTree = tree;
        text       = "Applications";
    }

    @Override
    public DesignTree designTree() {
        return designTree;
    }

    @Override
    public DesignNodeType nodeType() {
        return DesignNodeType.ApplicationRoot;
    }

}
