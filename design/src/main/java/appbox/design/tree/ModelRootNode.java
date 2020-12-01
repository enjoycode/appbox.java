package appbox.design.tree;

import appbox.data.PersistentState;
import appbox.design.utils.CodeHelper;
import appbox.model.ModelBase;
import appbox.model.ModelType;

import java.util.Collection;
import java.util.HashMap;

public final class ModelRootNode extends DesignNode {

    public final  ModelType                targetType;
    private final HashMap<Long, ModelNode> _models = new HashMap<>();

    public ModelRootNode(ModelType targetType) {
        this.targetType = targetType;
        text            = CodeHelper.getPluralStringOfModelType(targetType);
    }

    @Override
    public String id() {
        var appIdString = Integer.toUnsignedString(((ApplicationNode) getParent()).model.id());
        return String.format("%s-%s", appIdString, targetType.value);
    }

    @Override
    public DesignNodeType nodeType() {
        return DesignNodeType.ModelRootNode;
    }

    //region ====Add & Remove Child Methods====

    /**
     * 仅用于加载设计树时添加节点并绑定签出信息
     */
    protected ModelNode addModel(ModelBase model) {
        //注意：入参model可能被签出的本地替换掉，所以相关操作必须指向node.model()
        var tree = designTree();
        var node = new ModelNode(model, tree.designHub);
        tree.bindCheckoutInfo(node, model.persistentState() == PersistentState.Detached);

        //TODO:加入指定文件夹
        nodes.add(node);
        _models.put(node.model().id(), node); //加入字典表方便查找
        return node;
    }
    //endregion

    //region ====Find Methods====
    protected ModelNode findModelNodeByName(String name) {
        for (var n : _models.values()) {
            if (n.model().name().equals(name)) {
                return n;
            }
        }
        return null;
    }

    protected ModelNode findModelNode(long modelId) {
        return _models.get(modelId);
    }

    public Collection<ModelNode> getAllModelNodes() {
        return  _models.values();
    }
    //endregion
}
