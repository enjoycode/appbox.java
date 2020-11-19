package appbox.design.services;

import appbox.design.common.CheckoutInfo;
import appbox.design.common.CheckoutResult;
import appbox.design.tree.DesignNodeType;
import appbox.entities.Checkout;
import appbox.logging.Log;
import appbox.model.EntityModel;
import appbox.runtime.RuntimeContext;
import appbox.store.EntityStore;
import appbox.store.KVTransaction;
import appbox.store.ModelStore;
import appbox.store.query.TableScan;
import appbox.utils.IdUtil;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public final class CheckoutService
{

	/**
	 签出指定节点
	 */
	public static CompletableFuture<CheckoutResult> checkoutAsync(List<CheckoutInfo> checkoutInfos)
	{
		if (checkoutInfos == null || checkoutInfos.isEmpty())	{
			return null;
		}
		try{
			CheckoutResult result = new CheckoutResult(true);
			//尝试向存储插入签出信息
			return KVTransaction.beginAsync().thenCompose(txn->CompletableFuture.supplyAsync(()->{
				for(CheckoutInfo info :checkoutInfos){
					var obj = new Checkout();
					obj.setNodeType(info.getNodeType().value);
					obj.setTargetId(info.getTargetID());
					obj.setDeveloperId(info.getDeveloperOuid());
					obj.setDeveloperName(info.getDeveloperName());
					obj.setVersion(info.getVersion());
					EntityStore.insertEntityAsync(obj,txn);
				}
				return null;
			}).thenCompose(r->txn.commitAsync())).thenCompose(r->ModelStore.loadModelAsync(Long.parseLong(checkoutInfos.get(0).getTargetID())))
					.thenApply(r->{
						if(r.version()!=checkoutInfos.get(0).getVersion()){
							result.setModelWithNewVersion(r);
						}
						return result;
					});



		}catch (Exception e) {
			Log.error(e.getMessage());
			return CompletableFuture.completedFuture(new CheckoutResult(false));
		}

	}

	/**
	 用于DesignTree加载时
	 */
	public static CompletableFuture<Map<String,CheckoutInfo>> loadAllAsync(){
		var map = new HashMap<String, CheckoutInfo>();
		var q = new TableScan<>(IdUtil.SYS_CHECKOUT_MODEL_ID,Checkout.class);
		return q.toListAsync().thenApply(res->{
			if(res!=null&&res.size()>0){
				for(Checkout checkout:res){
					CheckoutInfo info = new CheckoutInfo(DesignNodeType.forValue(checkout.getNodeType()), checkout.getTargetId(), checkout.getVersion(), checkout.getDeveloperName(), checkout.getDeveloperId());
					map.put(info.getKey(), info);
				}
			}
			return map;
		});
	}

	/**
	 * 签入当前用户所有已签出项
	 */
	public static CompletableFuture<Void> checkInAsync(){
		return KVTransaction.beginAsync().thenCompose(txn->{

			var devId = RuntimeContext.current().currentSession().leafOrgUnitId();
			var model = (EntityModel)RuntimeContext.current().getModel(IdUtil.SYS_CHECKOUT_MODEL_ID);

			var q = new TableScan<>(IdUtil.SYS_CHECKOUT_MODEL_ID,Checkout.class);
			q.where(Checkout.DEVELOPER.eq(devId));
			return q.toListAsync().thenApply(res->{
				if(res!=null){
					for (Checkout checkout: res)
					{
						EntityStore.deleteEntityAsync(model, checkout.id(), txn);
					}
				}
				return null;
			}).thenCompose(r -> txn.commitAsync());
		});
	}

}
