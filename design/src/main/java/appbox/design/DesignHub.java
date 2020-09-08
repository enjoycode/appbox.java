package appbox.design;

import appbox.design.services.code.TypeSystem;
import appbox.design.tree.DesignTree;
import appbox.runtime.ISessionInfo;

public final class DesignHub {
    public final DesignTree designTree;
    public final TypeSystem typeSystem;

    public DesignHub(ISessionInfo session) {
        designTree = new DesignTree(this);
        typeSystem = new TypeSystem();
    }

}
