package appbox.channel;

public interface IMessageChannel {
    /**
     * 用于区分调试子进程
     */
    long getRemoteRuntimeId();
}
