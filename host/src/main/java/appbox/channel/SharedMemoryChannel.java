package appbox.channel;

/**
 * 与主进程通信的共享内存通道，每个实例包含两个单向消息队列
 */
public final class SharedMemoryChannel implements IMessageChannel {

    @Override
    public long getRemoteRuntimeId() {
        return 0;
    }

    /**
     * 开始在当前线程接收消息
     */
    public void startReceive() {

    }

}
