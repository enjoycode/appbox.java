package appbox.design.tree;

import appbox.design.common.CheckoutInfo;
import appbox.design.DesignHub;
import appbox.design.services.CheckoutService;
import appbox.design.services.StagedItems;
import appbox.design.services.StagedService;
import appbox.logging.Log;
import appbox.model.*;
import appbox.model.entity.EntityMemberModel;
import appbox.model.entity.EntityRefModel;
import appbox.runtime.RuntimeContext;
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

    public DataStoreRootNode storeRootNode() {
        return storeRootNode;
    }

    public ApplicationRootNode appRootNode() {
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

        //先加载签出信息及StagedModels
        return CheckoutService.loadAllAsync().thenCompose(checkouts -> {
            _checkouts = checkouts;
            return StagedService.loadStagedAsync(true);
        }).thenCompose(stagedItems -> {
            staged = stagedItems;
            return ModelStore.loadAllApplicationAsync();
        }).thenCompose(apps -> { //加载所有Apps
            for (ApplicationModel app : apps) {
                appRootNode.nodes.add(new ApplicationNode(this, app));
            }

            return ModelStore.loadAllModelAsync(); //加载所有模型
        }).thenCompose(models -> {
            var mergedModels = new ArrayList<ModelBase>(Arrays.asList(models));
            //加载staged中新建的模型，可能包含DataStoreModel
            mergedModels.addAll(Arrays.asList(staged.findNewModels()));

            //加入Models
            staged.removeDeletedModels(mergedModels);  //先移除已删除的
            var allModelNodes = new ArrayList<ModelNode>(); //需要延迟创建虚拟代码的模型
            for (ModelBase m : mergedModels) {
                if (m.modelType() == ModelType.DataStore) {
                    storeRootNode.addModel((DataStoreModel) m, designHub);
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

    /** 仅用于测试 */
    public void loadNodesForTest(ApplicationModel appModel, List<ModelBase> models) {
        nodes.clear();
        _checkouts = new HashMap<>();

        storeRootNode = new DataStoreRootNode(this);
        nodes.add(storeRootNode);
        appRootNode = new ApplicationRootNode(this);
        nodes.add(appRootNode);

        appRootNode.nodes.add(new ApplicationNode(this, appModel));
        var allModelNodes = new ArrayList<ModelNode>();
        for (ModelBase m : models) {
            if (m.modelType() == ModelType.DataStore) {
                storeRootNode.addModel((DataStoreModel) m, designHub);
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
            throw new RuntimeException(ex);
        }
    }
    //endregion

    //region ====Find Methods====

    /** 用于前端传回的参数查找对应的设计节点 */
    public DesignNode findNode(DesignNodeType type, String id) {
        switch (type) {
            case EntityModelNode:
                return findModelNode(ModelType.Entity, Long.parseUnsignedLong(id));
            case ServiceModelNode:
                return findModelNode(ModelType.Service, Long.parseUnsignedLong(id));
            case DataStoreNode:
                return storeRootNode.nodes.find(n -> n.id().equals(id));
            default:
                Log.warn("findNode: " + type.name() + "未实现");
                throw new RuntimeException("未实现");
        }
    }

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

    public DataStoreNode findDataStoreNodeByName(String name) {
        return (DataStoreNode) storeRootNode.nodes.find(n -> n.text().equals(name));
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

    /** 查找所有引用指定模型标识的EntityRef Member集合 */
    public List<EntityRefModel> findEntityRefModels(long targetEntityModelID) {
        List<EntityRefModel> result = new ArrayList<>();
        List<ModelNode>      ls     = findNodesByType(ModelType.Entity);

        for (ModelNode l : ls) {
            EntityModel model = (EntityModel) l.model();
            //注意：不能排除自身引用，主要指树状结构的实体
            for (int j = 0; j < model.getMembers().size(); j++) {
                if (model.getMembers().get(j).type() == EntityMemberModel.EntityMemberType.EntityRef) {
                    EntityRefModel refMember = (EntityRefModel) model.getMembers().get(j);
                    //注意不排除聚合引用
                    for (int k = 0; k < refMember.getRefModelIds().size(); k++) {
                        if (refMember.getRefModelIds().get(k) == targetEntityModelID)
                            result.add(refMember);
                    }
                }
            }
        }
        return result;
    }

    /** 查找指定类型的所有节点 */
    public List<ModelNode> findNodesByType(ModelType modelType) {
        var list = new ArrayList<ModelNode>();
        for (int i = 0; i < appRootNode.nodes.count(); i++) {
            var appNode       = (ApplicationNode) appRootNode.nodes.get(i);
            var modelRootNode = appNode.findModelRootNode(modelType);
            list.addAll(modelRootNode.getAllModelNodes());
        }
        return list;
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
        for (CheckoutInfo info : infos) {
            String key = CheckoutInfo.makeKey(info.nodeType, info.targetID);
            if (!_checkouts.containsKey(key)) {
                _checkouts.put(key, info);
            }
        }
    }

    /**
     * 给设计节点添加签出信息，如果已签出的模型节点则用本地存储替换原模型
     */
    protected void bindCheckoutInfo(DesignNode node, boolean isNewNode) {
        //if (node.NodeType == DesignNodeType.FolderNode || !node.AllowCheckout)
        //    throw new ArgumentException("不允许绑定签出信息: " + node.NodeType.ToString());

        //先判断是否新增的
        if (isNewNode) {
            node.setCheckoutInfo(new CheckoutInfo(node.nodeType(),
                    node.checkoutInfoTargetID(), node.version(),
                    designHub.session.name(), designHub.session.leafOrgUnitId()));
            return;
        }

        //非新增的比对服务端的签出列表
        var key      = CheckoutInfo.makeKey(node.nodeType(), node.checkoutInfoTargetID());
        var checkout = _checkouts.get(key);
        if (checkout != null) {
            node.setCheckoutInfo(checkout);
            if (node.isCheckoutByMe() && node instanceof ModelNode) { //如果是被当前用户签出的模型
                var modelNode = (ModelNode) node;
                //从staged加载修改中的模型进行替换
                var stagedModel = staged.findModel(modelNode.model().id());
                if (stagedModel != null)
                    modelNode.setModel(stagedModel);
            }
        }
    }

    /** 部署完后更新所有模型节点的状态，并移除待删除的节点 */
    public void checkinAllNodes() {
        //循环更新模型节点
        for (int i = 0; i < appRootNode.nodes.count(); i++) {
            ((ApplicationNode) appRootNode.nodes.get(i)).checkinAllNodes();
        }

        //刷新签出信息表，移除被自己签出的信息
        var list = new ArrayList<String>();
        for (var entry : _checkouts.entrySet()) {
            if (entry.getValue().developerOuid == RuntimeContext.current().currentSession().leafOrgUnitId())
                list.add(entry.getKey());
        }
        for (var key : list) {
            _checkouts.remove(key);
        }
    }
    //endregion

}
