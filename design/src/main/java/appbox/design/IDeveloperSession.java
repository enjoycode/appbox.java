package appbox.design;

public interface IDeveloperSession {

    /**
     * 获取当前用户会话的开发者的DesighHub实例
     */
    DesignHub getDesignHub();

    /**
     * 发送设计时事件
     */
    void sendEvent(int source, String body);

}
