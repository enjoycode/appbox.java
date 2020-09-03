package appbox.design.tree;

import appbox.design.common.CheckoutInfo;
import appbox.design.DesignHub;
import appbox.design.services.StagedItems;
import appbox.model.ApplicationModel;
import appbox.model.ModelBase;
import appbox.model.ModelType;
import appbox.store.ModelStore;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

public final class DesignTree {
    private final AtomicInteger _loadingFlag = new AtomicInteger(0);

    public final DesignHub      designHub;
    public final NodeCollection nodes;

    /**
     * 仅用于加载树时临时放入挂起的模型
     */
    private StagedItems         staged;
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
            for (ModelBase m : models) {
                if (m.modelType() == ModelType.DataStore) {
                    //TODO:
                } else {
                    findModelRootNode(m.appId(), m.modelType()).addModel(m);
                }
            }

            _loadingFlag.compareAndExchange(1, 0);
            return CompletableFuture.completedFuture(true);
        });
    }
    //endregion

    //region ====Find Methods====
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
    //endregion

    //region ====Checkout Methods====
    //用于签出节点成功后添加签出信息列表
    public void AddCheckoutInfos(List<CheckoutInfo> infos) {
        for (int i = 0; i < infos.size(); i++) {
            String key = CheckoutInfo.MakeKey(infos.get(i).getNodeType(), infos.get(i).getTargetID());
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
    //endregion

}
