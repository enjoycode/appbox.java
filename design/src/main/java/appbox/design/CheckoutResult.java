package appbox.design;

import appbox.*;
import appbox.model.ModelBase;

public final class CheckoutResult
{
	private boolean Success;
	public boolean getSuccess()
	{
		return Success;
	}

	/** 
	 签出单个模型时，已被其他人修改(版本变更), 则返回当前最新的版本的模型
	*/
	private ModelBase ModelWithNewVersion;
	public ModelBase getModelWithNewVersion()
	{
		return ModelWithNewVersion;
	}
	public void setModelWithNewVersion(ModelBase value)
	{
		ModelWithNewVersion = value;
	}

	public CheckoutResult(boolean success)
	{
		Success = success;
	}

}