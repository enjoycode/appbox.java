package appbox.design.services;

import appbox.design.common.CheckoutInfo;
import appbox.design.common.CheckoutResult;
import appbox.logging.Log;
import appbox.model.EntityModel;
import appbox.store.KVTransaction;
import appbox.store.ModelStore;
import appbox.utils.IdUtil;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public final class CheckoutService
{

	/** 
	 签出指定节点
	*/
	public static CompletableFuture<CheckoutResult> CheckoutAsync(List<CheckoutInfo> checkoutInfos)
	{
		if (checkoutInfos == null || checkoutInfos.isEmpty())	{
			return null;
		}
		try{
			//尝试向存储插入签出信息
			var model = new EntityModel(IdUtil.SYS_CHECKOUT_MODEL_ID, "Checkout", true, false);
			//TODO set model/batch insert
			return KVTransaction.beginAsync().thenCompose(txn-> ModelStore.insertModelAsync(model, txn).thenApply(r -> txn.commitAsync()).thenApply(r->{
				CheckoutResult result = new CheckoutResult(true);
				if (checkoutInfos.get(0).getIsSingleModel())
				{
					//var storedModel = await ModelStore.LoadModelAsync(Long.parseLong(checkoutInfos.get(0).getTargetID()));
					//if (storedModel.Version != checkoutInfos.get(0).getVersion())
					//{
					//	result.setModelWithNewVersion(storedModel);
					//}
				}
				return result;}));

		}catch (Exception e) {
			Log.error(e.getMessage());
			return CompletableFuture.completedFuture(new CheckoutResult(false));
		}
	//检查签出单个模型时，存储有无新版本

	}

	/** 
	 用于DesignTree加载时
	*/
//C# TO JAVA CONVERTER TODO TASK: There is no equivalent in Java to the 'async' keyword:
//ORIGINAL LINE: internal static async Task<Dictionary<string, CheckoutInfo>> LoadAllAsync()
/*	public static Task<HashMap<String, CheckoutInfo>> LoadAllAsync()
	{
		HashMap<String, CheckoutInfo> list = new HashMap<String, CheckoutInfo>();
//C# TO JAVA CONVERTER TODO TASK: There is no preprocessor in Java:
//#if FUTURE
		TableScan q = new TableScan(Consts.SYS_CHECKOUT_MODEL_ID);
//#else
		SqlQuery q = new SqlQuery(Consts.SYS_CHECKOUT_MODEL_ID);
//#endif
//C# TO JAVA CONVERTER TODO TASK: There is no equivalent to implicit typing in Java:
//C# TO JAVA CONVERTER TODO TASK: There is no equivalent to 'await' in Java:
		var res = await q.ToListAsync();
		if (res != null)
		{
			for (int i = 0; i < res.size(); i++)
			{
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: var info = new CheckoutInfo((DesignNodeType)res[i].GetByte(Consts.CHECKOUT_NODETYPE_ID), res[i].GetString(Consts.CHECKOUT_TARGETID_ID), (uint)res[i].GetInt32(Consts.CHECKOUT_VERSION_ID), res[i].GetString(Consts.CHECKOUT_DEVELOPERNAME_ID), res[i].GetGuid(Consts.CHECKOUT_DEVELOPERID_ID));
				CheckoutInfo info = new CheckoutInfo(DesignNodeType.forValue(res[i].GetByte(Consts.CHECKOUT_NODETYPE_ID)), res[i].GetString(Consts.CHECKOUT_TARGETID_ID), (int)res[i].GetInt32(Consts.CHECKOUT_VERSION_ID), res[i].GetString(Consts.CHECKOUT_DEVELOPERNAME_ID), res[i].GetGuid(Consts.CHECKOUT_DEVELOPERID_ID));
				list.put(info.GetKey(), info);
			}
		}

		return list;
	}

	*//**
	 签入当前用户所有已签出项
	*//*
//C# TO JAVA CONVERTER TODO TASK: Statements that are interrupted by preprocessor statements are not converted by C# to Java Converter:
	public static async Task CheckinAsync(
//C# TO JAVA CONVERTER TODO TASK: There is no preprocessor in Java:
//#if FUTURE
//C# TO JAVA CONVERTER TODO TASK: Statements that are interrupted by preprocessor statements are not converted by C# to Java Converter:
		private Transaction txn
//#else
//C# TO JAVA CONVERTER TODO TASK: Statements that are interrupted by preprocessor statements are not converted by C# to Java Converter:
		private System.Data.Common.DbTransaction txn
//#endif
//C# TO JAVA CONVERTER TODO TASK: Statements that are interrupted by preprocessor statements are not converted by C# to Java Converter:
	   private ) { var devId = RuntimeContext.getCurrent().getCurrentSession().getLeafOrgUnitID(); var model = await RuntimeContext.getCurrent().<EntityModel>GetModelAsync(Consts.SYS_CHECKOUT_MODEL_ID);

		//TODO:***** Use DeleteCommand(join txn), 暂临时使用查询再删除
//C# TO JAVA CONVERTER TODO TASK: There is no preprocessor in Java:
//#if FUTURE
		private TableScan q = new TableScan(Consts.SYS_CHECKOUT_MODEL_ID);
		q.Filter(appbox.Expressions.KVFieldExpression.OpEquality(q.GetGuid(Consts.CHECKOUT_DEVELOPERID_ID), devId));
//#else
		private SqlQuery q = new SqlQuery(Consts.SYS_CHECKOUT_MODEL_ID);
//C# TO JAVA CONVERTER TODO TASK: There is no Java equivalent to LINQ queries:
		q.Where(q.T["DeveloperId"] == devId);
//#endif

//C# TO JAVA CONVERTER TODO TASK: There is no equivalent to implicit typing in Java:
		private var list = await q.ToListAsync();

		private private if ()
		{
			return if (null);
		}

//C# TO JAVA CONVERTER NOTE: Java does not support optional parameters. Overloaded method(s) are created above:
//ORIGINAL LINE: if (list != null)
		private private if (list!)
		{
			for (int i = 0; i < list.size(); i++)
			{
//C# TO JAVA CONVERTER TODO TASK: There is no preprocessor in Java:
//#if FUTURE
//C# TO JAVA CONVERTER TODO TASK: There is no equivalent to 'await' in Java:
				await EntityStore.DeleteEntityAsync(model, list[i].Id, txn);
//#else
//C# TO JAVA CONVERTER TODO TASK: There is no equivalent to 'await' in Java:
				await SqlStore.getDefault().DeleteAsync(list[i], txn);
//#endif
			}
		}
}*/
}
