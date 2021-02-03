package appbox.design;

import appbox.runtime.IUserSession;

import java.util.concurrent.CompletableFuture;

public interface IDeveloperSession extends IUserSession {

    /** 获取当前用户会话的开发者的DesighHub实例 */
    DesignHub getDesignHub();

    /** 发送设计时事件 */
    void sendEvent(int source, String body);

    /**
     * 开启主进程与调试子进程的通道，并发送调用调试目标的请求
     * @param service    待调试的目标服务方法 eg: sys.OrderService.save
     * @param invokeArgs 已经序列化的调用参数，无参数为null
     */
    CompletableFuture<Void> startDebugChannel(String service, byte[] invokeArgs);

}
