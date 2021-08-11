package appbox.design.common;

import appbox.design.tree.ModelNode;

/** 模型对模型的引用,如EntityRef引用等 */
public final class ModelReference extends Reference {
    public ModelReference(ModelNode modelNode) {
        super(modelNode);
    }

    @Override
    public String location() {
        return null;
    }
}
