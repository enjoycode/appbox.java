package appbox.design.tree;

import appbox.design.utils.CodeHelper;
import appbox.model.ModelType;

public final class ModelRootNode extends DesignNode {

    public final ModelType targetType;

    public ModelRootNode(ModelType targetType) {
        this.targetType = targetType;
        text            = CodeHelper.getPluralStringOfModelType(targetType);
    }

    @Override
    public String id() {
        return ((ApplicationNode) getParent()).model.id() + "-" + targetType.value;
    }

    @Override
    public DesignNodeType nodeType() {
        return DesignNodeType.ModelRootNode;
    }
}
