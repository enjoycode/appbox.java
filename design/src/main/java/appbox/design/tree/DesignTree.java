package appbox.design.tree;

import appbox.design.common.CheckoutInfo;
import appbox.design.DesignHub;
import appbox.design.services.StagedItems;
import appbox.model.ApplicationModel;
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

    //用于签出节点成功后添加签出信息列表
    public void AddCheckoutInfos(List<CheckoutInfo> infos) {
        for (int i = 0; i < infos.size(); i++) {
            String key = CheckoutInfo.MakeKey(infos.get(i).getNodeType(), infos.get(i).getTargetID());
            if (!_checkouts.containsKey(key)) {
                _checkouts.put(key, infos.get(i));
            }
        }
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
        if (nodes.getCount() > 0) {
            nodes.Clear();
        }

        //开始加载
        storeRootNode = new DataStoreRootNode(this);
        nodes.Add(storeRootNode);
        appRootNode = new ApplicationRootNode(this);
        nodes.Add(appRootNode);

        //TODO: 先加载签出信息及StagedModels
        //TODO: 暂简单实现

        return ModelStore.loadAllApplicationAsync().thenApply(apps -> {
            for (ApplicationModel app : apps) {
                appRootNode.nodes.Add(new ApplicationNode(this, app));
            }

            _loadingFlag.compareAndExchange(1, 0);
            return true;
            //return ModelStore.loadAllModelAsync().thenApply(models -> {
            //    return true;
            //});
        });
    }
    //endregion

}
