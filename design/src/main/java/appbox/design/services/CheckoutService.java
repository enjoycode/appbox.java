package appbox.design.services;

import appbox.design.common.CheckoutInfo;
import appbox.design.common.CheckoutResult;
import appbox.design.tree.DesignNodeType;
import appbox.entities.Checkout;
import appbox.logging.Log;
import appbox.model.EntityModel;
import appbox.model.ModelBase;
import appbox.runtime.RuntimeContext;
import appbox.store.EntityStore;
import appbox.store.KVTransaction;
import appbox.store.ModelStore;
import appbox.store.query.TableScan;
import appbox.utils.IdUtil;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public final class CheckoutService {

    /**
     * 签出指定节点
     */
    public static CompletableFuture<CheckoutResult> checkoutAsync(List<CheckoutInfo> checkoutInfos) {
        if (checkoutInfos == null || checkoutInfos.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        return KVTransaction.beginAsync().thenCompose(txn -> {
            CompletableFuture<Void> future = null;
            for (CheckoutInfo info : checkoutInfos) {
                var obj = new Checkout();
                obj.setNodeType(info.nodeType.value);
                obj.setTargetId(info.targetID);
                obj.setDeveloperId(info.developerOuid);
                obj.setDeveloperName(info.developerName);
                obj.setVersion(info.version);
                if (future == null) {
                    future = EntityStore.insertEntityAsync(obj, txn);
                } else {
                    future = future.thenCompose(r -> EntityStore.insertEntityAsync(obj, txn));
                }
            }
            return future.thenCompose(r -> txn.commitAsync());
        }).thenCompose(r -> {
            //检查签出单个模型时，存储有无新版本
            if (checkoutInfos.get(0).isSingleModel()) {
                return ModelStore.loadModelAsync(Long.parseLong(checkoutInfos.get(0).targetID));
            } else {
                return CompletableFuture.completedFuture(null);
            }
        }).thenApply(r -> {
            ModelBase modelWithNewVersion = null;
            if (r != null && r.version() != checkoutInfos.get(0).version) {
                Log.debug("Checkout single model with new version.");
                modelWithNewVersion = r;
            }
            return new CheckoutResult(true, modelWithNewVersion);
        });
    }

    /**
     * 用于DesignTree加载时
     */
    public static CompletableFuture<Map<String, CheckoutInfo>> loadAllAsync() {
        var map = new HashMap<String, CheckoutInfo>();
        var q   = new TableScan<>(IdUtil.SYS_CHECKOUT_MODEL_ID, Checkout.class);
        return q.toListAsync().thenApply(res -> {
            if (res != null && res.size() > 0) {
                for (Checkout checkout : res) {
                    CheckoutInfo info = new CheckoutInfo(DesignNodeType.forValue(checkout.getNodeType()), checkout.getTargetId(), checkout.getVersion()
                            , checkout.getDeveloperName(), checkout.getDeveloperId());
                    map.put(info.getKey(), info);
                }
            }
            return map;
        });
    }

    /**
     * 签入当前用户所有已签出项
     */
    public static CompletableFuture<Void> checkInAsync() {
        var devId = RuntimeContext.current().currentSession().leafOrgUnitId();
        //UUID devId=new UUID(-6038453433195871438l,-7082168417221633763l);//测试用
        var model = (EntityModel) RuntimeContext.current().getModel(IdUtil.SYS_CHECKOUT_MODEL_ID);

        var q = new TableScan<>(IdUtil.SYS_CHECKOUT_MODEL_ID, Checkout.class);
        q.where(Checkout.DEVELOPER.eq(devId));
        return KVTransaction.beginAsync().thenCompose(txn->q.toListAsync().thenCompose(res ->{
            CompletableFuture future = null;
            if (res != null&&res.size()>0) {
                for (Checkout checkout : res) {
                    if (future == null) {
                        future = EntityStore.deleteEntityAsync(model, checkout.id(), txn);
                    } else {
                        future = future.thenCompose(r -> EntityStore.deleteEntityAsync(model, checkout.id(), txn));
                    }
                }
                future = future.thenCompose(r -> txn.commitAsync());
            }else{
                future = CompletableFuture.completedFuture(null);
            }
            return future;
        }));

    }

}
