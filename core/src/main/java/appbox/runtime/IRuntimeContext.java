package appbox.runtime;

import appbox.model.ApplicationModel;
import appbox.model.ModelBase;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 运行时上下文，用于提供模型容器及服务调用
 */
public interface IRuntimeContext {

    /**
     * 获取当前会话信息
     */
    ISessionInfo currentSession();

    /** 设置当前会话信息 */
    void setCurrentSession(ISessionInfo session);

    /**
     * 异步调用服务
     * @param method eg: "sys.OrderService.Save"
     */
    CompletableFuture<Object> invokeAsync(String method, List<InvokeArg> args);

    //region ====ModelContainer====
    //C#为异步，这里为了方便暂采用同步

    ApplicationModel getApplicationModel(int appId);

    <T extends ModelBase> T getModel(long modelId);

    /** 用于发布成功后更新模型缓存 */
    void invalidModelsCache(String[] services, long[] others, boolean byPublish);
    //endregion
}
