package appbox.design.tree;

import appbox.data.PersistentState;
import appbox.design.utils.CodeHelper;
import appbox.model.ModelBase;
import appbox.model.ModelFolder;
import appbox.model.ModelType;

import java.util.*;

public final class ModelRootNode extends DesignNode {

    public final ModelType   targetType;
    private      ModelFolder _rootFolder; //根文件夹

    private final Map<Long, ModelNode>  _models  = new HashMap<>();
    private final Map<UUID, FolderNode> _folders = new HashMap<>();

    public ModelRootNode(ModelType targetType) {
        this.targetType = targetType;
    }

    public String fullName() {
        return getParent().text();
    }

    @Override
    public String id() {
        var appIdString = Integer.toUnsignedString(((ApplicationNode) getParent()).model.id());
        return String.format("%s-%s", appIdString, targetType.value);
    }

    @Override
    public String text() {
        return CodeHelper.getPluralStringOfModelType(targetType);
    }

    @Override
    public String checkoutInfoTargetID() {
        return id();
    }

    @Override
    public DesignNodeType nodeType() {
        return DesignNodeType.ModelRootNode;
    }

    public ModelFolder rootFolder() {
        if (_rootFolder == null)
            _rootFolder = new ModelFolder(((ApplicationNode) getParent()).model.id(), targetType);
        return _rootFolder;
    }

    public boolean hasAnyModel() {
        return _models.size() > 0;
    }

    //region ====Add & Remove Child Methods====

    /** 仅用于设计树加载时从顶级开始递归添加文件夹节点 */
    protected void addFolder(ModelFolder folder, DesignNode parent) {
        DesignNode parentNode = this;
        if (folder.getParent() != null) {
            var node = new FolderNode(folder);
            parentNode = node;
            //不再检查本地有没有挂起的修改,由DesignTree加载时处理好
            if (parent == null)
                nodes.add(node);
            else
                parent.nodes.add(node);
            _folders.put(folder.id(), node);
        } else {
            _rootFolder = folder;
        }

        if (folder.hasChildren()) {
            for (var item : folder.children()) {
                addFolder(item, parentNode);
            }
        }
    }

    /** 用于新建时添加至字典表 */
    public void addFolderIndex(FolderNode node) {
        _folders.put(node.folder.id(), node);
    }

    /** 删除并移除字典表索引 */
    public void removeFolder(FolderNode node) {
        node.getParent().nodes.remove(node);
        _folders.remove(node.folder.id());
    }

    /** 仅用于加载设计树时添加节点并绑定签出信息 */
    protected ModelNode addModel(ModelBase model) {
        //注意：入参model可能被签出的本地替换掉，所以相关操作必须指向node.model()
        var tree = designTree();
        var node = new ModelNode(model, tree.designHub);
        tree.bindCheckoutInfo(node, model.persistentState() == PersistentState.Detached);

        var folderNode = _folders.get(node.model().getFolderId());
        if (folderNode == null)
            nodes.add(node);
        else
            folderNode.nodes.add(node);

        //加入字典表方便查找
        _models.put(node.model().id(), node);
        return node;
    }

    /** 用于新建时添加至字典表 */
    public void addModelIndex(ModelNode node) {
        _models.put(node.model().id(), node);
    }

    /** 删除并移除字典表中对应的键 */
    public void removeModel(ModelNode node) {
        node.getParent().nodes.remove(node);
        _models.remove(node.model().id());
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

    public ModelNode findModelNode(long modelId) {
        return _models.get(modelId);
    }

    public FolderNode findFolderNode(UUID folderId) {
        return _folders.get(folderId);
    }

    public Collection<ModelNode> getAllModelNodes() {
        return _models.values();
    }
    //endregion

    //region ====Checkin====
    void checkinAllNodes() {
        //定义待删除模型节点列表
        var deletes = new ArrayList<ModelNode>();

        //签入模型根节点，文件夹的签出信息同模型根节点
        if (isCheckoutByMe())
            setCheckoutInfo(null);

        //签入所有模型节点
        for (var modelNode : _models.values()) {
            if (modelNode.isCheckoutByMe()) {
                //判断是否待删除的节点
                if (modelNode.model().persistentState() == PersistentState.Deleted) {
                    deletes.add(modelNode);
                } else {
                    modelNode.setCheckoutInfo(null);
                    //不再需要累加版本号，由ModelStore保存模型时处理
                    modelNode.model().acceptChanges();
                }
            }
        }

        //TODO:移除已删除的文件夹节点

        //移除待删除的模型节点
        for (var deletedNode : deletes) {
            _models.remove(deletedNode.model().id()); //先移除索引
            deletedNode.getParent().nodes.remove(deletedNode); //再移除节点
        }
    }
    //endregion

}
