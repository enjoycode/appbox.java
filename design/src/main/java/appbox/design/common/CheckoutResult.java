package appbox.design.common;

import appbox.model.ModelBase;

public final class CheckoutResult {
    public final boolean   success;
    /**
     * 签出单个模型时，已被其他人修改(版本变更), 则返回当前最新的版本的模型
     */
    public final ModelBase modelWithNewVersion;

    public CheckoutResult(boolean success, ModelBase modelWithNewVersion) {
        this.success             = success;
        this.modelWithNewVersion = modelWithNewVersion;
    }

}