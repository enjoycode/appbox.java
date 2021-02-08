package appbox.design;

import appbox.channel.IClientMessage;
import appbox.runtime.IUserSession;

import java.util.concurrent.CompletableFuture;

public interface IDeveloperSession extends IUserSession {
    /** 调试事件标识 */
    int DEBUG_EVENT = 12;

    /** 获取当前用户会话的开发者的DesighHub实例 */
    DesignHub getDesignHub();

    /** 发送设计时事件 */
    void sendEvent(IClientMessage event);

    /**
     * 开启主进程与调试子进程的通道，并发送调用调试目标的请求
     * @param service    待调试的目标服务方法 eg: sys.OrderService.save
     * @param invokeArgs 已经序列化的调用参数，无参数为null
     */
    CompletableFuture<Void> startDebugChannel(String service, byte[] invokeArgs);

    /** 获取序列化的数据，用于调试时写入会话信息至调试目录内 */
    byte[] getSerializedData();

}
