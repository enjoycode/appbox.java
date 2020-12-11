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
        nodes.add(modelRoot);
        tree.bindCheckoutInfo(modelRoot, false);

        modelRoot = new ModelRootNode(ModelType.Service);
        nodes.add(modelRoot);
        tree.bindCheckoutInfo(modelRoot, false);

        modelRoot = new ModelRootNode(ModelType.View);
        nodes.add(modelRoot);
        tree.bindCheckoutInfo(modelRoot, false);

        modelRoot = new ModelRootNode(ModelType.Workflow);
        nodes.add(modelRoot);
        tree.bindCheckoutInfo(modelRoot, false);

        modelRoot = new ModelRootNode(ModelType.Report);
        nodes.add(modelRoot);
        tree.bindCheckoutInfo(modelRoot, false);

        modelRoot = new ModelRootNode(ModelType.Enum);
        nodes.add(modelRoot);
        tree.bindCheckoutInfo(modelRoot, false);

        modelRoot = new ModelRootNode(ModelType.Event);
        nodes.add(modelRoot);
        tree.bindCheckoutInfo(modelRoot, false);

        modelRoot = new ModelRootNode(ModelType.Permission);
        nodes.add(modelRoot);
        tree.bindCheckoutInfo(modelRoot, false);
    }

    @Override
    public DesignNodeType nodeType() {
        return DesignNodeType.ApplicationNode;
    }

    public ModelRootNode findModelRootNode(ModelType modelType) {
        for (DesignNode node : nodes.list) {
            if (node instanceof ModelRootNode && ((ModelRootNode) node).targetType == modelType) {
                return (ModelRootNode) node;
            }
        }
        return null;
    }

    /** 签入当前应用节点下所有子节点 */
    void checkinAllNodes() {
        for (int i = 0; i < nodes.count(); i++) {
            if (nodes.get(i) instanceof ModelRootNode)
                ((ModelRootNode) nodes.get(i)).checkinAllNodes();
        }
    }

}
