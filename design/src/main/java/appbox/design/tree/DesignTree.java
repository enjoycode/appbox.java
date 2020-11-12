package appbox.design.tree;

import appbox.design.common.CheckoutInfo;
import appbox.design.DesignHub;
import appbox.design.services.StagedItems;
import appbox.model.*;
import appbox.model.entity.EntityMemberModel;
import appbox.model.entity.EntityRefModel;
import appbox.store.ModelStore;
import appbox.utils.IdUtil;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

public final class DesignTree {
    private final AtomicInteger _loadingFlag = new AtomicInteger(0);

    public final DesignHub      designHub;
    public final NodeCollection nodes;

    private StagedItems         staged;       //仅用于加载树时临时放入挂起的模型
    private DataStoreRootNode   storeRootNode;
    private ApplicationRootNode appRootNode;

    private Map<String, CheckoutInfo> _checkouts;

    public DesignTree(DesignHub hub) {
        designHub = hub;
        nodes     = new NodeCollection(null);
    }

    //region ====Properties====
    public StagedItems getStaged() {
        return staged;
    }

    public void setStaged(StagedItems value) {
        staged = value;
    }

    public DataStoreRootNode getStoreRootNode() {
        return storeRootNode;
    }

    public ApplicationRootNode getAppRootNode() {
        return appRootNode;
    }
    //endregion

    //region ====Load Methods====
    public CompletableFuture<Boolean> loadNodesAsync() {
        if (_loadingFlag.compareAndExchange(0, 1) != 0) {
            return CompletableFuture.failedFuture(new Exception("DesignTree is loading."));
        }

        //先判断是否已经加载过，是则清空准备重新加载
        if (nodes.count() > 0) {
            nodes.clear();
        }

        //开始加载
        storeRootNode = new DataStoreRootNode(this);
        nodes.add(storeRootNode);
        appRootNode = new ApplicationRootNode(this);
        nodes.add(appRootNode);

        //TODO: 先加载签出信息及StagedModels
        //TODO: 暂简单实现

        return ModelStore.loadAllApplicationAsync().thenCompose(apps -> { //加载所有Apps
            for (ApplicationModel app : apps) {
                appRootNode.nodes.add(new ApplicationNode(this, app));
            }

            return ModelStore.loadAllModelAsync(); //加载所有模型
        }).thenCompose(models -> {
            //TODO:先移除已删除的

            var allModelNodes = new ArrayList<ModelNode>();
            for (ModelBase m : models) {
                if (m.modelType() == ModelType.DataStore) {
                    //TODO:
                } else {
                    allModelNodes.add(findModelRootNode(m.appId(), m.modelType()).addModel(m));
                }
            }

            //在所有节点加载完后创建模型对应的虚拟文件
            try {
                for (ModelNode n : allModelNodes) {
                    designHub.typeSystem.createModelDocument(n);
                }
            } catch (Exception ex) {
                return CompletableFuture.failedFuture(ex);
            } finally {
                _loadingFlag.compareAndExchange(1, 0);
            }

            return CompletableFuture.completedFuture(true);
        });
    }
    //endregion

    //region ====Find Methods====
    public ApplicationNode findApplicationNodeByName(String name) {
        for (DesignNode node : appRootNode.nodes.list) {
            if (((ApplicationNode) node).model.name().equals(name)) {
                return (ApplicationNode) node;
            }
        }
        return null;
    }

    public ApplicationNode findApplicationNode(int appId) {
        for (DesignNode node : appRootNode.nodes.list) {
            if (((ApplicationNode) node).model.id() == appId) {
                return (ApplicationNode) node;
            }
        }
        return null;
    }

    public ModelRootNode findModelRootNode(int appId, ModelType modelType) {
        for (DesignNode node : appRootNode.nodes.list) {
            var appNode = (ApplicationNode) node;
            if (appNode.model.id() == appId) {
                return appNode.findModelRootNode(modelType);
            }
        }
        return null;
    }

    /**
     * 根据模型标识获取相应的节点
     */
    public final ModelNode findModelNode(long modelId) {
        return findModelNode(IdUtil.getModelTypeFromModelId(modelId), modelId);
    }

    /**
     * 根据模型类型及标识号获取相应的节点
     */
    public final ModelNode findModelNode(ModelType modelType, long modelId) {
        var appId         = IdUtil.getAppIdFromModelId(modelId);
        var modelRootNode = findModelRootNode(appId, modelType);
        if (modelRootNode == null) {
            return null;
        }
        return modelRootNode.findModelNode(modelId);
    }
    //endregion

    //region ====Find for Create====

    /**
     * 用于新建时检查相同名称的模型是否已存在
     */
    public ModelNode findModelNodeByName(int appId, ModelType type, String name) {
        //TODO:***** 考虑在这里加载存储有没有相同名称的存在,或发布时检测，如改为全局Workspace没有此问题
        // dev1 -> load tree -> checkout -> add model -> publish
        // dev2 -> load tree                                 -> checkout -> add model with same name will pass
        var modelRootNode = findModelRootNode(appId, type);
        return modelRootNode.findModelNodeByName(name);
    }
    //endregion

    //region ====Checkout Methods====
    //用于签出节点成功后添加签出信息列表
    public void addCheckoutInfos(List<CheckoutInfo> infos) {
        for (int i = 0; i < infos.size(); i++) {
            String key = CheckoutInfo.makeKey(infos.get(i).getNodeType(), infos.get(i).getTargetID());
            if (!_checkouts.containsKey(key)) {
                _checkouts.put(key, infos.get(i));
            }
        }
    }

    /**
     * 给设计节点添加签出信息，如果已签出的模型节点则用本地存储替换原模型
     */
    protected void bindCheckoutInfo(DesignNode node, boolean isNewNode) {
        //TODO:
    }

    /**
     * 查找所有引用指定模型标识的EntityRef Member集合
     * @param targetEntityModelID
     * @return
     */
    public List<EntityRefModel> findEntityRefModels(long targetEntityModelID) {
        List result=new ArrayList();
        List<ModelNode> ls=findNodesByType(ModelType.Entity);

        for (int i = 0; i < ls.size(); i++)
        {
            EntityModel model = (EntityModel)ls.get(i).model();
            //注意：不能排除自身引用，主要指树状结构的实体
            for (int j = 0; j < model.getMembers().size(); j++)
            {
                if (model.getMembers().get(j).type() == EntityMemberModel.EntityMemberType.EntityRef)
                {
                    EntityRefModel refMember = (EntityRefModel)model.getMembers().get(j);
                    //注意不排除聚合引用
                    for (int k = 0; k < refMember.getRefModelIds().size(); k++)
                    {
                        if (refMember.getRefModelIds().get(k) == targetEntityModelID)
                            result.add(refMember);
                    }
                }
            }
        }
        return result;
    }

    public List<ModelNode> findNodesByType(ModelType modelType)
    {
        var list = new ArrayList<ModelNode>();
        for (int i = 0; i < appRootNode.nodes.count(); i++)
        {
            var appNode = (ApplicationNode)appRootNode.nodes.get(i);
            var modelRootNode = appNode.findModelRootNode(modelType);
            list.addAll(modelRootNode.getAllModelNodes());
        }
        return list;
    }
    //endregion

}
