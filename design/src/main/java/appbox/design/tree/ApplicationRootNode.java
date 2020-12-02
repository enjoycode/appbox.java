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

    /** 签入当前应用节点下所有子节点 */
    void checkinAllNodes() {
        for (int i = 0; i < nodes.count(); i++) {
            if (nodes.get(i) instanceof ModelRootNode)
                ((ModelRootNode) nodes.get(i)).checkinAllNodes();
        }
    }

}
