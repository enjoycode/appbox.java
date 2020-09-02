package appbox.design.tree;

import appbox.model.ApplicationModel;
import appbox.model.ModelType;

public final class ApplicationNode extends DesignNode {

    public final ApplicationModel model;

    public ApplicationNode(DesignTree tree, ApplicationModel model) {
        this.model = model;
        text       = model.name();

        //添加各类模型的根节点
        var modelRoot = new ModelRootNode(ModelType.Entity);
        nodes.Add(modelRoot);
        //tree.bindCheckoutInfo(modelRoot, false);

        modelRoot = new ModelRootNode(ModelType.Service);
        nodes.Add(modelRoot);

        modelRoot = new ModelRootNode(ModelType.View);
        nodes.Add(modelRoot);

        modelRoot = new ModelRootNode(ModelType.Workflow);
        nodes.Add(modelRoot);

        modelRoot = new ModelRootNode(ModelType.Report);
        nodes.Add(modelRoot);

        modelRoot = new ModelRootNode(ModelType.Enum);
        nodes.Add(modelRoot);

        modelRoot = new ModelRootNode(ModelType.Event);
        nodes.Add(modelRoot);

        modelRoot = new ModelRootNode(ModelType.Permission);
        nodes.Add(modelRoot);
    }

    @Override
    public DesignNodeType nodeType() {
        return DesignNodeType.ApplicationNode;
    }
}
