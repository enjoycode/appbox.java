package appbox.design.tree;

public final class ApplicationRootNode extends DesignNode implements ITopNode {

    private final DesignTree designTree;

    public ApplicationRootNode(DesignTree tree) {
        designTree = tree;
    }

    @Override
    public DesignTree designTree() {
        return designTree;
    }

    @Override
    public DesignNodeType nodeType() {
        return DesignNodeType.ApplicationRoot;
    }

    @Override
    public String text() {return "Applications";}
}
