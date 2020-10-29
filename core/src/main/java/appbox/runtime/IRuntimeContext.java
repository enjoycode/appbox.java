package appbox.runtime;

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

    /**
     * 设置当前会话信息
     * @param session
     */
    void setCurrentSession(ISessionInfo session);

    /**
     * 异步调用服务
     *
     * @param method eg: "sys.OrderService.Save"
     */
    CompletableFuture<Object> invokeAsync(String method, List<InvokeArg> args);

}
