package appbox.design;

import appbox.model.ModelBase;

public final class CheckoutResult
{
	private boolean success;
	/**
	 签出单个模型时，已被其他人修改(版本变更), 则返回当前最新的版本的模型
	 */
	private ModelBase modelWithNewVersion;


	public boolean getSuccess() {
		return success;
	}
	public void setSuccess(boolean success) {
		this.success = success;
	}
	public ModelBase getModelWithNewVersion()
	{
		return modelWithNewVersion;
	}
	public void setModelWithNewVersion(ModelBase value)
	{
		modelWithNewVersion = value;
	}
	public CheckoutResult(boolean success)
	{
		success = success;
	}

}