package appbox.design.tree;

public abstract class DesignNode {
    //region ====NodeCollection====
    final class NodeCollection {

    }
    //endregion

    private DesignNode _parent;

    //region ====Properties====
    public DesignNode getParent() {
        return _parent;
    }

    public void setParent(DesignNode parent) {
        _parent = parent;
    }
    //endregion
}
