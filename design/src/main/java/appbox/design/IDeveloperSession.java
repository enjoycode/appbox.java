package appbox.design;

import appbox.runtime.ISessionInfo;

public interface IDeveloperSession extends ISessionInfo {

    /**
     * 获取当前用户会话的开发者的DesighHub实例
     */
    DesignHub getDesignHub();

    /**
     * 发送设计时事件
     */
    void sendEvent(int source, String body);

}
