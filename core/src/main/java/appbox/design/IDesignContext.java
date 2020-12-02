package appbox.design;

import appbox.model.ApplicationModel;
import appbox.model.EntityModel;

/** 设计时上下文，每个开发者对应一个实例 */
public interface IDesignContext {
    ApplicationModel getApplicationModel(int appId);

    EntityModel getEntityModel(long modelId);
}
