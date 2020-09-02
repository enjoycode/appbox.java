package appbox.design;

import appbox.design.tree.DesignTree;
import appbox.runtime.ISessionInfo;

public final class DesignHub {
    public final DesignTree designTree;

    public DesignHub(ISessionInfo session) {
        designTree = new DesignTree(this);
    }

}
