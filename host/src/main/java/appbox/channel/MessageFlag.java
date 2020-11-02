package appbox.channel;

public final class MessageFlag {
    public static final byte None           = 0;
    public static final byte FirstChunk     = 4;
    public static final byte LastChunk      = 8;
    public static final byte SerializeError = 32; // 向通道写入消息时序列化失败，标记消息通知接收端作相应的处理
}
